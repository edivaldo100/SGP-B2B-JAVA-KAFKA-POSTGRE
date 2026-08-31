package com.edivaldo.pedido.domain.port.out;

import com.edivaldo.pedido.domain.model.IdempotencyRecord;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRepository {

    // Tenta inserir — retorna true se ganhou a corrida, false se a chave ja existia
    boolean tryInsert(IdempotencyRecord record);

    // Tenta reclamar um registro PROCESSING expirado — retorna true se atualizou 1 linha
    boolean reclaimExpired(IdempotencyRecord newRecord);

    Optional<IdempotencyRecord> findByPartnerIdAndKey(UUID partnerId, String idempotencyKey);

    void markDone(UUID id, UUID orderId, int responseStatus, String responseBody);
}
