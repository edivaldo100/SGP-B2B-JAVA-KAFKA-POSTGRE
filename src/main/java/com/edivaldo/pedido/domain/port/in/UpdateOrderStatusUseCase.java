package com.edivaldo.pedido.domain.port.in;

import com.edivaldo.pedido.application.command.UpdateOrderStatusCommand;
import com.edivaldo.pedido.application.dto.OrderResponse;

public interface UpdateOrderStatusUseCase {
    OrderResponse execute(UpdateOrderStatusCommand command);
}
