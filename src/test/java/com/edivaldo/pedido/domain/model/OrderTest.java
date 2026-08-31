package com.edivaldo.pedido.domain.model;

import com.edivaldo.pedido.domain.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrderTest {

    private static final UUID PARTNER_ID = UUID.randomUUID();

    private Order buildOrder() {
        List<OrderItem> items = List.of(
                new OrderItem("PROD-001", 2, new BigDecimal("100.00")),
                new OrderItem("PROD-002", 1, new BigDecimal("50.00"))
        );
        return Order.create(PARTNER_ID, items);
    }

    @Test
    void create_deveCalcularTotalCorreto() {
        Order order = buildOrder();

        assertThat(order.getTotalAmount()).isEqualByComparingTo("250.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDENTE);
        assertThat(order.getPartnerId()).isEqualTo(PARTNER_ID);
        assertThat(order.getItems()).hasSize(2);
    }

    @Test
    void approve_deveMudarStatusParaAprovado() {
        Order order = buildOrder();
        order.approve();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.APROVADO);
    }

    @Test
    void approve_deveRejeitarQuandoNaoPendente() {
        Order order = buildOrder();
        order.approve();

        assertThatThrownBy(order::approve)
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("APROVADO -> APROVADO");
    }

    @Test
    void maquinaDeEstados_transicaoCompletar() {
        Order order = buildOrder();
        order.approve();
        order.startProcessing();
        order.ship();
        order.deliver();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.ENTREGUE);
    }

    @Test
    void cancel_deveFuncionarEmQualquerEstadoAntesDeEntregue() {
        Order pendente = buildOrder();
        pendente.cancel();
        assertThat(pendente.getStatus()).isEqualTo(OrderStatus.CANCELADO);

        Order aprovado = buildOrder();
        aprovado.approve();
        aprovado.cancel();
        assertThat(aprovado.getStatus()).isEqualTo(OrderStatus.CANCELADO);
    }

    @Test
    void cancel_deveRejeitarQuandoEntregue() {
        Order order = buildOrder();
        order.approve();
        order.startProcessing();
        order.ship();
        order.deliver();

        assertThatThrownBy(order::cancel)
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("ENTREGUE");
    }

    @Test
    void cancel_deveRejeitarQuandoJaCancelado() {
        Order order = buildOrder();
        order.cancel();

        assertThatThrownBy(order::cancel)
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void isCreditDebitable_deveRetornarTrueApenasParaAprovadoEEmProcessamento() {
        Order pendente = buildOrder();
        assertThat(pendente.isCreditDebitable()).isFalse();

        Order aprovado = buildOrder();
        aprovado.approve();
        assertThat(aprovado.isCreditDebitable()).isTrue();

        Order emProcessamento = buildOrder();
        emProcessamento.approve();
        emProcessamento.startProcessing();
        assertThat(emProcessamento.isCreditDebitable()).isTrue();

        Order enviado = buildOrder();
        enviado.approve();
        enviado.startProcessing();
        enviado.ship();
        assertThat(enviado.isCreditDebitable()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"EM_PROCESSAMENTO", "ENVIADO", "ENTREGUE", "CANCELADO"})
    void approve_deveRejeitarQuandoStatusInvalido(OrderStatus status) {
        Order order = buildOrder();
        // forcar status via reconstitute para testar a validacao
        Order reconstituted = Order.reconstitute(order.getId(), order.getPartnerId(),
                order.getItems(), order.getTotalAmount(), status,
                order.getCreatedAt(), order.getUpdatedAt());

        assertThatThrownBy(reconstituted::approve)
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void create_deveGerarIdUnico() {
        Order o1 = buildOrder();
        Order o2 = buildOrder();
        assertThat(o1.getId()).isNotEqualTo(o2.getId());
    }

    @Test
    void getItems_deveRetornarListaImutavel() {
        Order order = buildOrder();
        assertThatThrownBy(() -> order.getItems().add(new OrderItem("PROD-X", 1, BigDecimal.ONE)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
