package com.edivaldo.pedido.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(
    UUID partnerId,
    String idempotencyKey,
    String requestHash,
    List<Item> items
) {
    public record Item(String productId, int quantity, BigDecimal unitPrice) {}
}
