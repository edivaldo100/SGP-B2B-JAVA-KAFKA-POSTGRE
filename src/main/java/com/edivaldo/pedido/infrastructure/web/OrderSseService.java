package com.edivaldo.pedido.infrastructure.web;

import com.edivaldo.pedido.application.dto.OrderResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class OrderSseService {

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public OrderSseService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        log.info("SSE: novo cliente conectado, total={}", emitters.size());
        return emitter;
    }

    // AFTER_COMMIT: só dispara após a transação commitar com sucesso.
    // Exceções aqui nunca afetam a transação de negócio.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        broadcast(event.order());
    }

    private void broadcast(OrderResponse order) {
        String data;
        try {
            data = objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar order para SSE", e);
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("order").data(data));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }
}
