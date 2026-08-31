package com.edivaldo.pedido.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class OutboxEvent {

    public enum Status { PENDING, PUBLISHED, FAILED }

    private UUID id;
    private UUID aggregateId;
    private String eventType;
    private String payload;
    private Status status;
    private int retryCount;
    private LocalDateTime nextAttemptAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;

    public static OutboxEvent create(UUID aggregateId, String eventType, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.id = UUID.randomUUID();
        e.aggregateId = aggregateId;
        e.eventType = eventType;
        e.payload = payload;
        e.status = Status.PENDING;
        e.retryCount = 0;
        e.createdAt = LocalDateTime.now();
        e.nextAttemptAt = e.createdAt;
        return e;
    }

    private OutboxEvent() {}

    public UUID getId()            { return id; }
    public UUID getAggregateId()   { return aggregateId; }
    public String getEventType()   { return eventType; }
    public String getPayload()     { return payload; }
    public Status getStatus()      { return status; }
    public int getRetryCount()     { return retryCount; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public String getLastError()   { return lastError; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
}
