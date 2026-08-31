package com.edivaldo.pedido.domain.port.in;

import com.edivaldo.pedido.application.dto.OrderResponse;
import com.edivaldo.pedido.domain.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FindOrderUseCase {

    OrderResponse findById(UUID id);

    List<OrderResponse> findByPartnerId(UUID partnerId);

    List<OrderResponse> findByStatus(OrderStatus status);

    List<OrderResponse> findByPeriod(LocalDateTime start, LocalDateTime end);
}
