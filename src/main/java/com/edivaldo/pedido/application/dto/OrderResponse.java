package com.edivaldo.pedido.application.dto;

import com.edivaldo.pedido.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID partnerId,
    Integer partnerSequentialId,
    String partnerName,
    List<OrderItemResponse> items,
    BigDecimal totalAmount,
    OrderStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public record OrderItemResponse(
        UUID id,
        String productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
    ) {}
}
