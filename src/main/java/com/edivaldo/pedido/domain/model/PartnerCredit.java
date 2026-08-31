package com.edivaldo.pedido.domain.model;

import com.edivaldo.pedido.domain.exception.InsufficientCreditException;

import java.math.BigDecimal;
import java.util.UUID;

public class PartnerCredit {

    private UUID partnerId;
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;

    public static PartnerCredit of(UUID partnerId, BigDecimal creditLimit, BigDecimal availableCredit) {
        PartnerCredit pc = new PartnerCredit();
        pc.partnerId = partnerId;
        pc.creditLimit = creditLimit;
        pc.availableCredit = availableCredit;
        return pc;
    }

    private PartnerCredit() {}

    // ─── Regras de negócio ────────────────────────────────────────────────────

    public boolean hasCredit(BigDecimal amount) {
        return availableCredit.compareTo(amount) >= 0;
    }

    public void debit(BigDecimal amount) {
        if (!hasCredit(amount)) {
            throw new InsufficientCreditException(
                "Credito insuficiente para o parceiro " + partnerId +
                ". Disponivel: " + availableCredit + ", Solicitado: " + amount);
        }
        this.availableCredit = this.availableCredit.subtract(amount);
    }

    public void release(BigDecimal amount) {
        this.availableCredit = this.availableCredit.add(amount);
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public UUID getPartnerId()            { return partnerId; }
    public BigDecimal getCreditLimit()    { return creditLimit; }
    public BigDecimal getAvailableCredit(){ return availableCredit; }
}
