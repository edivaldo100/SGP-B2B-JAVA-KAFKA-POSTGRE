package com.edivaldo.pedido.infrastructure.persistence.adapter;

import com.edivaldo.pedido.domain.model.Order;
import com.edivaldo.pedido.domain.model.OrderItem;
import com.edivaldo.pedido.domain.model.OrderStatus;
import com.edivaldo.pedido.domain.port.out.OrderRepository;
import com.edivaldo.pedido.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.edivaldo.pedido.infrastructure.persistence.entity.OrderJpaEntity;
import com.edivaldo.pedido.infrastructure.persistence.jpa.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = toEntity(order);
        OrderJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Order> findByPartnerId(UUID partnerId) {
        return jpaRepository.findByPartnerId(partnerId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return jpaRepository.findByStatus(status).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByCreatedAtBetween(start, end).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Order> findByPartnerIdAndStatus(UUID partnerId, OrderStatus status) {
        return jpaRepository.findByPartnerIdAndStatus(partnerId, status).stream().map(this::toDomain).toList();
    }

    // ─── Mapeamentos ──────────────────────────────────────────────────────────

    private OrderJpaEntity toEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(order.getId());
        entity.setPartnerId(order.getPartnerId());
        entity.setTotalAmount(order.getTotalAmount());
        entity.setStatus(order.getStatus());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt());

        List<OrderItemJpaEntity> itemEntities = order.getItems().stream()
                .map(item -> toItemEntity(item, entity))
                .toList();
        entity.getItems().clear();
        entity.getItems().addAll(itemEntities);
        return entity;
    }

    private OrderItemJpaEntity toItemEntity(OrderItem item, OrderJpaEntity orderEntity) {
        OrderItemJpaEntity entity = new OrderItemJpaEntity();
        entity.setId(item.getId());
        entity.setOrder(orderEntity);
        entity.setProductId(item.getProductId());
        entity.setQuantity(item.getQuantity());
        entity.setUnitPrice(item.getUnitPrice());
        return entity;
    }

    private Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(i -> new OrderItem(i.getId(), i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .toList();

        return Order.reconstitute(
                entity.getId(),
                entity.getPartnerId(),
                items,
                entity.getTotalAmount(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
