package com.edivaldo.pedido.domain.port.in;

import com.edivaldo.pedido.application.dto.OrderResponse;

import java.util.UUID;

public interface CancelOrderUseCase {
    OrderResponse execute(UUID orderId);
}
