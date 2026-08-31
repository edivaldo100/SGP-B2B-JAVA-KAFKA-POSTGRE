package com.edivaldo.pedido.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreditTransactionResponse(
    UUID id,
    UUID partnerId,
    UUID orderId,
    String type,
    BigDecimal amount,
    LocalDateTime createdAt
) {}
