package com.edivaldo.pedido.infrastructure.persistence.adapter;

import com.edivaldo.pedido.domain.model.IdempotencyRecord;
import com.edivaldo.pedido.domain.port.out.IdempotencyRepository;
import com.edivaldo.pedido.infrastructure.persistence.entity.IdempotencyJpaEntity;
import com.edivaldo.pedido.infrastructure.persistence.jpa.IdempotencyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IdempotencyRepositoryAdapter implements IdempotencyRepository {

    private final IdempotencyJpaRepository jpaRepository;

    @Override
    public boolean tryInsert(IdempotencyRecord record) {
        try {
            IdempotencyJpaEntity entity = toEntity(record);
            jpaRepository.saveAndFlush(entity);
            return true;
        } catch (DataIntegrityViolationException e) {
            // unique constraint (partner_id, idempotency_key) violada — outra instancia ganhou
            return false;
        }
    }

    @Override
    public Optional<IdempotencyRecord> findByPartnerIdAndKey(UUID partnerId, String idempotencyKey) {
        return jpaRepository.findByPartnerIdAndIdempotencyKey(partnerId, idempotencyKey)
                .map(this::toDomain);
    }

    @Override
    public void markDone(UUID id, UUID orderId, int responseStatus, String responseBody) {
        jpaRepository.markDone(id, orderId, responseStatus, responseBody, LocalDateTime.now());
    }

    private IdempotencyJpaEntity toEntity(IdempotencyRecord record) {
        IdempotencyJpaEntity entity = new IdempotencyJpaEntity();
        entity.setId(record.getId());
        entity.setPartnerId(record.getPartnerId());
        entity.setIdempotencyKey(record.getIdempotencyKey());
        entity.setRequestHash(record.getRequestHash());
        entity.setStatus(record.getStatus());
        entity.setOrderId(record.getOrderId());
        entity.setResponseStatus(record.getResponseStatus());
        entity.setResponseBody(record.getResponseBody());
        entity.setCreatedAt(record.getCreatedAt());
        entity.setCompletedAt(record.getCompletedAt());
        entity.setExpiresAt(record.getExpiresAt());
        return entity;
    }

    private IdempotencyRecord toDomain(IdempotencyJpaEntity entity) {
        return IdempotencyRecord.reconstitute(
                entity.getId(),
                entity.getPartnerId(),
                entity.getIdempotencyKey(),
                entity.getRequestHash(),
                entity.getStatus(),
                entity.getOrderId(),
                entity.getResponseStatus(),
                entity.getResponseBody(),
                entity.getCreatedAt(),
                entity.getCompletedAt(),
                entity.getExpiresAt()
        );
    }
}
