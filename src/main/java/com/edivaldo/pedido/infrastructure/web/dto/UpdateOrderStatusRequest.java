package com.edivaldo.pedido.infrastructure.web.dto;

import com.edivaldo.pedido.domain.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Status e obrigatorio")
        OrderStatus status
) {}
