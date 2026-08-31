package com.edivaldo.pedido.application.service;

import com.edivaldo.pedido.application.command.CreateOrderCommand;
import com.edivaldo.pedido.application.dto.OrderResponse;
import com.edivaldo.pedido.domain.exception.DuplicateOperationException;
import com.edivaldo.pedido.domain.exception.InsufficientCreditException;
import com.edivaldo.pedido.domain.exception.PartnerNotFoundException;
import com.edivaldo.pedido.domain.model.IdempotencyRecord;
import com.edivaldo.pedido.domain.model.Order;
import com.edivaldo.pedido.domain.model.OrderItem;
import com.edivaldo.pedido.domain.model.OutboxEvent;
import com.edivaldo.pedido.domain.model.PartnerCredit;
import com.edivaldo.pedido.domain.port.in.CreateOrderUseCase;
import com.edivaldo.pedido.domain.port.out.IdempotencyRepository;
import com.edivaldo.pedido.domain.port.out.OrderRepository;
import com.edivaldo.pedido.domain.port.out.OutboxEventRepository;
import com.edivaldo.pedido.domain.port.out.PartnerCreditRepository;
import com.edivaldo.pedido.infrastructure.web.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateOrderService implements CreateOrderUseCase {

    private static final int IDEMPOTENCY_TTL_MINUTES = 30;

    private final OrderRepository orderRepository;
    private final PartnerCreditRepository partnerCreditRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderMapper orderMapper;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public OrderResponse execute(CreateOrderCommand command) {
        // Tx1: registra PROCESSING e commita imediatamente
        // Isso garante que outras instancias vejam PROCESSING e falhem rapido
        IdempotencyRecord record = transactionTemplate.execute(status -> insertProcessingRecord(command));

        if (record == null) {
            throw new IllegalStateException("Falha ao registrar idempotency record");
        }

        // Tx2: logica de negocio completa + DONE
        return transactionTemplate.execute(status -> executeBusinessLogic(command, record));
    }

    private IdempotencyRecord insertProcessingRecord(CreateOrderCommand command) {
        Optional<IdempotencyRecord> existing = idempotencyRepository
                .findByPartnerIdAndKey(command.partnerId(), command.idempotencyKey());

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();

            if (record.isHashMismatch(command.requestHash())) {
                throw new DuplicateOperationException(
                        "Idempotency key ja utilizada com payload diferente: " + command.idempotencyKey());
            }

            if (record.isDone()) {
                return record;
            }

            if (!record.isExpired()) {
                throw new DuplicateOperationException(
                        "Pedido ainda esta sendo processado: " + command.idempotencyKey());
            }

            // expirado — reclaima atomicamente; se outro processo ganhou a corrida, 409
            IdempotencyRecord reclaimed = IdempotencyRecord.createProcessing(
                    command.partnerId(),
                    command.idempotencyKey(),
                    command.requestHash(),
                    IDEMPOTENCY_TTL_MINUTES
            );
            if (!idempotencyRepository.reclaimExpired(reclaimed)) {
                throw new DuplicateOperationException(
                        "Pedido concorrente detectado: " + command.idempotencyKey());
            }
            return reclaimed;
        }

        IdempotencyRecord newRecord = IdempotencyRecord.createProcessing(
                command.partnerId(),
                command.idempotencyKey(),
                command.requestHash(),
                IDEMPOTENCY_TTL_MINUTES
        );

        boolean won = idempotencyRepository.tryInsert(newRecord);
        if (!won) {
            throw new DuplicateOperationException(
                    "Pedido concorrente detectado: " + command.idempotencyKey());
        }
        return newRecord;
    }

    private OrderResponse executeBusinessLogic(CreateOrderCommand command, IdempotencyRecord idempotencyRecord) {
        if (idempotencyRecord.isDone()) {
            Order existing = orderRepository.findById(idempotencyRecord.getOrderId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Pedido nao encontrado para idempotency record: " + idempotencyRecord.getId()));
            return orderMapper.toResponse(existing);
        }

        List<OrderItem> items = command.items().stream()
                .map(i -> new OrderItem(i.productId(), i.quantity(), i.unitPrice()))
                .toList();

        Order order = Order.create(command.partnerId(), items);

        // Verificação antecipada de crédito — sem lock, sem debit.
        // O debit efetivo ocorre ao aprovar o pedido (PENDENTE → APROVADO).
        PartnerCredit credit = partnerCreditRepository
                .findByPartnerId(command.partnerId())
                .orElseThrow(() -> new PartnerNotFoundException(command.partnerId()));

        if (!credit.hasCredit(order.getTotalAmount())) {
            throw new InsufficientCreditException(
                    String.format("Credito insuficiente para parceiro %s: necessario %s, disponivel %s",
                            command.partnerId(), order.getTotalAmount(), credit.getAvailableCredit()));
        }

        Order saved = orderRepository.save(order);

        outboxEventRepository.save(OutboxEvent.create(
                saved.getId(),
                "ORDER_CREATED",
                buildPayload(saved)
        ));

        idempotencyRepository.markDone(idempotencyRecord.getId(), saved.getId(), 201, null);

        OrderResponse response = orderMapper.toResponse(saved);
        eventPublisher.publishEvent(new OrderCreatedEvent(response));
        return response;
    }

    private String buildPayload(Order order) {
        return String.format(
                "{\"orderId\":\"%s\",\"partnerId\":\"%s\",\"totalAmount\":%s,\"status\":\"%s\"}",
                order.getId(), order.getPartnerId(), order.getTotalAmount(), order.getStatus()
        );
    }
}
