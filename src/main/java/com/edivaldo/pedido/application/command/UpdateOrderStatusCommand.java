package com.edivaldo.pedido.application.command;

import com.edivaldo.pedido.domain.model.OrderStatus;

import java.util.UUID;

public record UpdateOrderStatusCommand(UUID orderId, OrderStatus newStatus) {}
