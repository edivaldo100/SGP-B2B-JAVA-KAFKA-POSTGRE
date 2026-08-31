package com.edivaldo.pedido.infrastructure.web;

import com.edivaldo.pedido.application.dto.OrderResponse;

public record OrderCreatedEvent(OrderResponse order) {}
