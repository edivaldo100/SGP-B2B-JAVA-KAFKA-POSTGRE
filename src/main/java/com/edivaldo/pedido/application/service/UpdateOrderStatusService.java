package com.edivaldo.pedido.application.service;

import com.edivaldo.pedido.application.command.UpdateOrderStatusCommand;
import com.edivaldo.pedido.application.dto.OrderResponse;
import com.edivaldo.pedido.domain.exception.OrderNotFoundException;
import com.edivaldo.pedido.domain.model.Order;
import com.edivaldo.pedido.domain.model.OutboxEvent;
import com.edivaldo.pedido.domain.model.OrderStatus;
import com.edivaldo.pedido.domain.port.in.UpdateOrderStatusUseCase;
import com.edivaldo.pedido.domain.port.out.OrderRepository;
import com.edivaldo.pedido.domain.port.out.OutboxEventRepository;
import com.edivaldo.pedido.domain.port.out.PartnerCreditRepository;
import com.edivaldo.pedido.infrastructure.web.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateOrderStatusService implements UpdateOrderStatusUseCase {

    private final OrderRepository orderRepository;
    private final PartnerCreditRepository partnerCreditRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public OrderResponse execute(UpdateOrderStatusCommand command) {
        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        boolean wasDebitable = order.isCreditDebitable();
        applyTransition(order, command.newStatus());

        // devolve credito se cancelado apos debitado
        if (command.newStatus() == OrderStatus.CANCELADO && wasDebitable) {
            partnerCreditRepository.findByPartnerIdForUpdate(order.getPartnerId())
                    .ifPresent(credit -> {
                        credit.release(order.getTotalAmount());
                        partnerCreditRepository.save(credit);
                    });
        }

        Order updated = orderRepository.save(order);

        outboxEventRepository.save(OutboxEvent.create(
                updated.getId(),
                resolveEventType(command.newStatus()),
                buildPayload(updated)
        ));

        OrderResponse response = orderMapper.toResponse(updated);
        eventPublisher.publishEvent(new OrderCreatedEvent(response));
        return response;
    }

    private void applyTransition(Order order, OrderStatus target) {
        switch (target) {
            case APROVADO -> order.approve();
            case EM_PROCESSAMENTO -> order.startProcessing();
            case ENVIADO -> order.ship();
            case ENTREGUE -> order.deliver();
            case CANCELADO -> order.cancel();
            default -> throw new IllegalArgumentException("Status invalido: " + target);
        }
    }

    private String resolveEventType(OrderStatus status) {
        return switch (status) {
            case APROVADO -> "ORDER_APPROVED";
            case EM_PROCESSAMENTO -> "ORDER_PROCESSING";
            case ENVIADO -> "ORDER_SHIPPED";
            case ENTREGUE -> "ORDER_DELIVERED";
            case CANCELADO -> "ORDER_CANCELLED";
            default -> "ORDER_STATUS_CHANGED";
        };
    }

    private String buildPayload(Order order) {
        return String.format(
                "{\"orderId\":\"%s\",\"partnerId\":\"%s\",\"status\":\"%s\"}",
                order.getId(), order.getPartnerId(), order.getStatus()
        );
    }
}
