package com.edivaldo.pedido.domain.port.out;

import com.edivaldo.pedido.domain.model.Order;
import com.edivaldo.pedido.domain.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    List<Order> findByPartnerId(UUID partnerId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Order> findByPartnerIdAndStatus(UUID partnerId, OrderStatus status);
}
