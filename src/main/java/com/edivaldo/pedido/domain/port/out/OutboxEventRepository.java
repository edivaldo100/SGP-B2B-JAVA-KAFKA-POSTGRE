package com.edivaldo.pedido.domain.port.out;

import com.edivaldo.pedido.domain.model.OutboxEvent;

import java.util.List;

public interface OutboxEventRepository {

    void save(OutboxEvent event);

    // Busca com FOR UPDATE SKIP LOCKED para processamento concorrente seguro
    List<OutboxEvent> findPendingForUpdate(int limit);

    void markPublished(java.util.UUID id);

    void markFailed(java.util.UUID id, String error, int retryCount);
}
