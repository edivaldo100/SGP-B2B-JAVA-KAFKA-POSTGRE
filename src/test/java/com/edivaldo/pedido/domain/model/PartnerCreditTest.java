package com.edivaldo.pedido.domain.model;

import com.edivaldo.pedido.domain.exception.InsufficientCreditException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PartnerCreditTest {

    private final UUID PARTNER_ID = UUID.randomUUID();

    private PartnerCredit credit(double limit, double available) {
        return PartnerCredit.of(PARTNER_ID, BigDecimal.valueOf(limit), BigDecimal.valueOf(available));
    }

    @Test
    void hasCredit_deveRetornarTrueQuandoSuficiente() {
        PartnerCredit pc = credit(1000, 500);
        assertThat(pc.hasCredit(new BigDecimal("500.00"))).isTrue();
        assertThat(pc.hasCredit(new BigDecimal("499.99"))).isTrue();
    }

    @Test
    void hasCredit_deveRetornarFalseQuandoInsuficiente() {
        PartnerCredit pc = credit(1000, 100);
        assertThat(pc.hasCredit(new BigDecimal("100.01"))).isFalse();
    }

    @Test
    void debit_deveReduzirCreditoDisponivel() {
        PartnerCredit pc = credit(1000, 500);
        pc.debit(new BigDecimal("200.00"));
        assertThat(pc.getAvailableCredit()).isEqualByComparingTo("300.00");
    }

    @Test
    void debit_deveLancarExcecaoQuandoInsuficiente() {
        PartnerCredit pc = credit(1000, 100);
        assertThatThrownBy(() -> pc.debit(new BigDecimal("100.01")))
                .isInstanceOf(InsufficientCreditException.class);
    }

    @Test
    void release_deveAumentarCreditoDisponivel() {
        PartnerCredit pc = credit(1000, 300);
        pc.release(new BigDecimal("200.00"));
        assertThat(pc.getAvailableCredit()).isEqualByComparingTo("500.00");
    }

    @Test
    void debitERelease_devemSerInversos() {
        PartnerCredit pc = credit(1000, 1000);
        BigDecimal valor = new BigDecimal("350.75");
        pc.debit(valor);
        assertThat(pc.getAvailableCredit()).isEqualByComparingTo("649.25");
        pc.release(valor);
        assertThat(pc.getAvailableCredit()).isEqualByComparingTo("1000.00");
    }

    @Test
    void debit_exatamenteNaFronteira_devePermitir() {
        PartnerCredit pc = credit(500, 500);
        pc.debit(new BigDecimal("500.00"));
        assertThat(pc.getAvailableCredit()).isEqualByComparingTo("0.00");
    }
}
