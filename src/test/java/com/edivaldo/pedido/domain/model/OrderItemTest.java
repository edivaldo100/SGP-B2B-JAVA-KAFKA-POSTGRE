package com.edivaldo.pedido.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class OrderItemTest {

    @Test
    void subtotal_deveMultiplicarQuantidadePorPreco() {
        OrderItem item = new OrderItem("PROD-001", 3, new BigDecimal("25.50"));
        assertThat(item.subtotal()).isEqualByComparingTo("76.50");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void constructor_deveRejeitarQuantidadeInvalida(int quantidade) {
        assertThatThrownBy(() -> new OrderItem("PROD-001", quantidade, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maior que zero");
    }

    @Test
    void constructor_deveRejeitarPrecoNegativo() {
        assertThatThrownBy(() -> new OrderItem("PROD-001", 1, new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_devePermitirPrecoZero() {
        OrderItem item = new OrderItem("PROD-GRATIS", 1, BigDecimal.ZERO);
        assertThat(item.subtotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void constructor_deveGerarIdUnico() {
        OrderItem i1 = new OrderItem("P1", 1, BigDecimal.ONE);
        OrderItem i2 = new OrderItem("P1", 1, BigDecimal.ONE);
        assertThat(i1.getId()).isNotEqualTo(i2.getId());
    }
}
