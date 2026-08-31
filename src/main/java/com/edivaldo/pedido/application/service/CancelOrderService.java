package com.edivaldo.pedido.application.service;

import com.edivaldo.pedido.application.dto.OrderResponse;
import com.edivaldo.pedido.domain.exception.OrderNotFoundException;
import com.edivaldo.pedido.domain.model.Order;
import com.edivaldo.pedido.domain.model.OutboxEvent;
import com.edivaldo.pedido.domain.model.PartnerCredit;
import com.edivaldo.pedido.domain.port.in.CancelOrderUseCase;
import com.edivaldo.pedido.domain.port.out.OrderRepository;
import com.edivaldo.pedido.domain.port.out.OutboxEventRepository;
import com.edivaldo.pedido.domain.port.out.PartnerCreditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final PartnerCreditRepository partnerCreditRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponse execute(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        boolean wasDebitable = order.isCreditDebitable();
        order.cancel();

        if (wasDebitable) {
            partnerCreditRepository.findByPartnerIdForUpdate(order.getPartnerId())
                    .ifPresent(credit -> {
                        credit.release(order.getTotalAmount());
                        partnerCreditRepository.save(credit);
                    });
        }

        Order cancelled = orderRepository.save(order);

        outboxEventRepository.save(OutboxEvent.create(
                cancelled.getId(),
                "ORDER_CANCELLED",
                String.format("{\"orderId\":\"%s\",\"partnerId\":\"%s\",\"status\":\"CANCELADO\"}",
                        cancelled.getId(), cancelled.getPartnerId())
        ));

        return orderMapper.toResponse(cancelled);
    }
}
