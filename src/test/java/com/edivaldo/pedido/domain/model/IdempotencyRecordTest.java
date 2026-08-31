package com.edivaldo.pedido.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class IdempotencyRecordTest {

    private static final UUID PARTNER_ID = UUID.randomUUID();
    private static final String KEY = "idem-key-123";
    private static final String HASH = "abc123hash";

    @Test
    void createProcessing_deveIniciarComStatusProcessing() {
        IdempotencyRecord record = IdempotencyRecord.createProcessing(PARTNER_ID, KEY, HASH, 30);

        assertThat(record.getStatus()).isEqualTo(IdempotencyRecord.Status.PROCESSING);
        assertThat(record.isDone()).isFalse();
        assertThat(record.isExpired()).isFalse();
        assertThat(record.getExpiresAt()).isAfter(record.getCreatedAt());
    }

    @Test
    void isHashMismatch_deveRetornarTrueParaHashDiferente() {
        IdempotencyRecord record = IdempotencyRecord.createProcessing(PARTNER_ID, KEY, HASH, 30);
        assertThat(record.isHashMismatch("outro-hash")).isTrue();
        assertThat(record.isHashMismatch(HASH)).isFalse();
    }

    @Test
    void isExpired_deveRetornarFalseQuandoDentroDoTTL() {
        IdempotencyRecord record = IdempotencyRecord.createProcessing(PARTNER_ID, KEY, HASH, 30);
        assertThat(record.isExpired()).isFalse();
    }

    @Test
    void reconstitute_devePreservarTodosOsCampos() {
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        IdempotencyRecord record = IdempotencyRecord.reconstitute(
                id, PARTNER_ID, KEY, HASH,
                IdempotencyRecord.Status.DONE, orderId,
                201, null,
                java.time.LocalDateTime.now().minusMinutes(1),
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now().plusMinutes(29)
        );

        assertThat(record.getId()).isEqualTo(id);
        assertThat(record.getOrderId()).isEqualTo(orderId);
        assertThat(record.isDone()).isTrue();
        assertThat(record.getResponseStatus()).isEqualTo(201);
    }
}
