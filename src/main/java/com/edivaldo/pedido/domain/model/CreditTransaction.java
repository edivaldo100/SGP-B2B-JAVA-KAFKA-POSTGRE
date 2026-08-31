package com.edivaldo.pedido.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class CreditTransaction {

    private UUID id;
    private UUID partnerId;
    private UUID orderId;
    private CreditTransactionType type;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    public static CreditTransaction debit(UUID partnerId, UUID orderId, BigDecimal amount) {
        return build(partnerId, orderId, CreditTransactionType.DEBIT, amount);
    }

    public static CreditTransaction release(UUID partnerId, UUID orderId, BigDecimal amount) {
        return build(partnerId, orderId, CreditTransactionType.RELEASE, amount);
    }

    private static CreditTransaction build(UUID partnerId, UUID orderId,
                                           CreditTransactionType type, BigDecimal amount) {
        CreditTransaction t = new CreditTransaction();
        t.id = UUID.randomUUID();
        t.partnerId = partnerId;
        t.orderId = orderId;
        t.type = type;
        t.amount = amount;
        t.createdAt = LocalDateTime.now();
        return t;
    }

    public static CreditTransaction reconstitute(UUID id, UUID partnerId, UUID orderId,
                                                  CreditTransactionType type, BigDecimal amount,
                                                  LocalDateTime createdAt) {
        CreditTransaction t = new CreditTransaction();
        t.id = id;
        t.partnerId = partnerId;
        t.orderId = orderId;
        t.type = type;
        t.amount = amount;
        t.createdAt = createdAt;
        return t;
    }

    private CreditTransaction() {}

    public UUID getId()                    { return id; }
    public UUID getPartnerId()             { return partnerId; }
    public UUID getOrderId()               { return orderId; }
    public CreditTransactionType getType() { return type; }
    public BigDecimal getAmount()          { return amount; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
}
