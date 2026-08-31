package com.edivaldo.pedido.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PartnerResponse(
    Integer id,
    UUID partnerUuid,
    String name,
    LocalDateTime createdAt
) {}
