package com.edivaldo.pedido.infrastructure.persistence.jpa;

import com.edivaldo.pedido.infrastructure.persistence.entity.IdempotencyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyJpaRepository extends JpaRepository<IdempotencyJpaEntity, UUID> {

    Optional<IdempotencyJpaEntity> findByPartnerIdAndIdempotencyKey(UUID partnerId, String idempotencyKey);

    @Modifying
    @Query("""
        UPDATE IdempotencyJpaEntity i
        SET i.status = 'DONE', i.orderId = :orderId,
            i.responseStatus = :responseStatus, i.responseBody = :responseBody,
            i.completedAt = :completedAt
        WHERE i.id = :id
    """)
    void markDone(UUID id, UUID orderId, int responseStatus, String responseBody, LocalDateTime completedAt);
}
