package com.edivaldo.pedido.infrastructure.web;

import com.edivaldo.pedido.application.dto.OrderResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
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
        SseEmitter emitter = new SseEmitter(0L); // sem timeout
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        log.info("SSE: novo cliente conectado, total={}", emitters.size());
        return emitter;
    }

    @EventListener
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
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
