package com.edivaldo.pedido.infrastructure.persistence.jpa;

import com.edivaldo.pedido.domain.model.OrderStatus;
import com.edivaldo.pedido.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {

    List<OrderJpaEntity> findByPartnerId(UUID partnerId);

    List<OrderJpaEntity> findByStatus(OrderStatus status);

    List<OrderJpaEntity> findByPartnerIdAndStatus(UUID partnerId, OrderStatus status);

    @Query("SELECT o FROM OrderJpaEntity o WHERE o.createdAt BETWEEN :start AND :end")
    List<OrderJpaEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
