package com.edivaldo.pedido.infrastructure.persistence.jpa;

import com.edivaldo.pedido.infrastructure.persistence.entity.IdempotencyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Reclaima um registro PROCESSING expirado de forma atômica.
    // Só atualiza se expires_at < NOW() e status = PROCESSING — garante que apenas
    // um processo vence quando há concorrência sobre a mesma chave expirada.
    @Modifying
    @Query(value = """
        UPDATE idempotency_keys
        SET    id              = :newId,
               request_hash   = :requestHash,
               status         = 'PROCESSING',
               created_at     = :createdAt,
               expires_at     = :expiresAt,
               order_id       = NULL,
               response_status = NULL,
               response_body  = NULL,
               completed_at   = NULL
        WHERE  partner_id      = :partnerId
          AND  idempotency_key = :idempotencyKey
          AND  expires_at      < NOW()
          AND  status          = 'PROCESSING'
        """, nativeQuery = true)
    int reclaimExpired(
            @Param("newId") UUID newId,
            @Param("requestHash") String requestHash,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("expiresAt") LocalDateTime expiresAt,
            @Param("partnerId") UUID partnerId,
            @Param("idempotencyKey") String idempotencyKey);
}
