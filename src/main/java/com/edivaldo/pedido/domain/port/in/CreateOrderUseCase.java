package com.edivaldo.pedido.domain.port.in;

import com.edivaldo.pedido.application.command.CreateOrderCommand;
import com.edivaldo.pedido.application.dto.OrderResponse;

public interface CreateOrderUseCase {
    OrderResponse execute(CreateOrderCommand command);
}
