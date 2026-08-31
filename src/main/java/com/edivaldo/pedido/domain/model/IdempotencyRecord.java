package com.edivaldo.pedido.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class IdempotencyRecord {

    public enum Status { PROCESSING, DONE }

    private UUID id;
    private UUID partnerId;
    private String idempotencyKey;
    private String requestHash;
    private Status status;
    private UUID orderId;
    private Integer responseStatus;
    private String responseBody;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;

    public static IdempotencyRecord createProcessing(UUID partnerId, String idempotencyKey,
                                                      String requestHash, int ttlMinutes) {
        IdempotencyRecord r = new IdempotencyRecord();
        r.id = UUID.randomUUID();
        r.partnerId = partnerId;
        r.idempotencyKey = idempotencyKey;
        r.requestHash = requestHash;
        r.status = Status.PROCESSING;
        r.createdAt = LocalDateTime.now();
        r.expiresAt = r.createdAt.plusMinutes(ttlMinutes);
        return r;
    }

    public static IdempotencyRecord reconstitute(UUID id, UUID partnerId, String idempotencyKey,
                                                  String requestHash, Status status, UUID orderId,
                                                  Integer responseStatus, String responseBody,
                                                  LocalDateTime createdAt, LocalDateTime completedAt,
                                                  LocalDateTime expiresAt) {
        IdempotencyRecord r = new IdempotencyRecord();
        r.id = id;
        r.partnerId = partnerId;
        r.idempotencyKey = idempotencyKey;
        r.requestHash = requestHash;
        r.status = status;
        r.orderId = orderId;
        r.responseStatus = responseStatus;
        r.responseBody = responseBody;
        r.createdAt = createdAt;
        r.completedAt = completedAt;
        r.expiresAt = expiresAt;
        return r;
    }

    private IdempotencyRecord() {}

    public boolean isHashMismatch(String incomingHash) {
        return !this.requestHash.equals(incomingHash);
    }

    public boolean isDone() {
        return status == Status.DONE;
    }

    public boolean isExpired() {
        return status == Status.PROCESSING && LocalDateTime.now().isAfter(expiresAt);
    }

    public UUID getId()               { return id; }
    public UUID getPartnerId()        { return partnerId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash()    { return requestHash; }
    public Status getStatus()         { return status; }
    public UUID getOrderId()          { return orderId; }
    public Integer getResponseStatus(){ return responseStatus; }
    public String getResponseBody()   { return responseBody; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getExpiresAt()   { return expiresAt; }
}
