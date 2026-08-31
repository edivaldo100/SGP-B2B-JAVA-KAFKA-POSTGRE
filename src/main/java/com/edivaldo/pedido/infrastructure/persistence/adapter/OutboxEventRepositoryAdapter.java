package com.edivaldo.pedido.infrastructure.persistence.adapter;

import com.edivaldo.pedido.domain.model.OutboxEvent;
import com.edivaldo.pedido.domain.port.out.OutboxEventRepository;
import com.edivaldo.pedido.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.edivaldo.pedido.infrastructure.persistence.jpa.OutboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {

    private static final long BASE_BACKOFF_SECONDS = 30L;

    private final OutboxEventJpaRepository jpaRepository;

    @Override
    public void save(OutboxEvent event) {
        jpaRepository.save(toEntity(event));
    }

    @Override
    public List<OutboxEvent> findPendingForUpdate(int limit) {
        return jpaRepository.findPendingForUpdate(limit).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void markPublished(UUID id) {
        jpaRepository.markPublished(id, LocalDateTime.now());
    }

    @Override
    public void markFailed(UUID id, String error, int retryCount) {
        OutboxEvent.Status status = retryCount >= OutboxEvent.MAX_RETRIES
                ? OutboxEvent.Status.FAILED
                : OutboxEvent.Status.PENDING;

        // backoff exponencial: 30s, 60s, 120s, 240s, 480s
        long backoffSeconds = BASE_BACKOFF_SECONDS * (1L << Math.min(retryCount, 4));
        LocalDateTime nextAttemptAt = LocalDateTime.now().plusSeconds(backoffSeconds);

        jpaRepository.markFailed(id, status, error, nextAttemptAt);
    }

    private OutboxEventJpaEntity toEntity(OutboxEvent event) {
        OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
        entity.setId(event.getId());
        entity.setAggregateId(event.getAggregateId());
        entity.setEventType(event.getEventType());
        entity.setPayload(event.getPayload());
        entity.setStatus(event.getStatus());
        entity.setRetryCount(event.getRetryCount());
        entity.setNextAttemptAt(event.getNextAttemptAt());
        entity.setLastError(event.getLastError());
        entity.setCreatedAt(event.getCreatedAt());
        entity.setPublishedAt(event.getPublishedAt());
        return entity;
    }

    private OutboxEvent toDomain(OutboxEventJpaEntity entity) {
        return OutboxEvent.reconstitute(
                entity.getId(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getRetryCount(),
                entity.getNextAttemptAt(),
                entity.getLastError(),
                entity.getCreatedAt(),
                entity.getPublishedAt()
        );
    }
}
