package com.edivaldo.pedido.infrastructure.persistence.jpa;

import com.edivaldo.pedido.domain.model.OutboxEvent;
import com.edivaldo.pedido.infrastructure.persistence.entity.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    @Query(value = """
        SELECT * FROM outbox_events
        WHERE status = 'PENDING' AND next_attempt_at <= NOW()
        ORDER BY next_attempt_at
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
    """, nativeQuery = true)
    List<OutboxEventJpaEntity> findPendingForUpdate(int limit);

    @Modifying
    @Query("""
        UPDATE OutboxEventJpaEntity o
        SET o.status = 'PUBLISHED', o.publishedAt = :publishedAt
        WHERE o.id = :id
    """)
    void markPublished(UUID id, LocalDateTime publishedAt);

    @Modifying
    @Query("""
        UPDATE OutboxEventJpaEntity o
        SET o.status = :status, o.retryCount = o.retryCount + 1,
            o.lastError = :lastError, o.nextAttemptAt = :nextAttemptAt
        WHERE o.id = :id
    """)
    void markFailed(UUID id, OutboxEvent.Status status, String lastError, LocalDateTime nextAttemptAt);
}
