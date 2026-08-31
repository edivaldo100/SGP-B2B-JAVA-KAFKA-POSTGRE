package com.edivaldo.pedido.application.service;

import com.edivaldo.pedido.application.dto.OrderResponse;
import com.edivaldo.pedido.domain.model.Order;
import com.edivaldo.pedido.domain.model.OrderItem;
import com.edivaldo.pedido.domain.model.Partner;
import com.edivaldo.pedido.domain.port.out.PartnerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final PartnerRepository partnerRepository;

    public OrderResponse toResponse(Order order) {
        Optional<Partner> partner = partnerRepository.findByPartnerUuid(order.getPartnerId());

        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getPartnerId(),
                partner.map(Partner::getId).orElse(null),
                partner.map(Partner::getName).orElse(null),
                items,
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderResponse.OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderResponse.OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.subtotal()
        );
    }
}
