package com.edivaldo.pedido.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class KafkaSseService {

    private static final int MAX_HISTORY = 20;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final Deque<KafkaEventDto> history = new ArrayDeque<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record KafkaEventDto(String topic, String orderId, String partnerId, String status, String receivedAt) {}

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        // Envia histórico dos últimos 20 eventos para o novo subscriber
        List<KafkaEventDto> snapshot;
        synchronized (history) {
            snapshot = new ArrayList<>(history);
        }
        for (KafkaEventDto event : snapshot) {
            sendToEmitter(emitter, event);
        }

        return emitter;
    }

    public void broadcast(String topic, String payload) {
        KafkaEventDto event = parse(topic, payload);
        if (event == null) return;

        synchronized (history) {
            history.addFirst(event);
            while (history.size() > MAX_HISTORY) {
                history.removeLast();
            }
        }

        for (SseEmitter emitter : emitters) {
            sendToEmitter(emitter, event);
        }
    }

    private void sendToEmitter(SseEmitter emitter, KafkaEventDto event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event().name("kafka").data(json));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
    }

    @SuppressWarnings("unchecked")
    private KafkaEventDto parse(String topic, String payload) {
        try {
            var map = objectMapper.readValue(payload, java.util.Map.class);
            return new KafkaEventDto(
                topic,
                String.valueOf(map.getOrDefault("orderId", "")),
                String.valueOf(map.getOrDefault("partnerId", "")),
                String.valueOf(map.getOrDefault("status", "")),
                Instant.now().toString()
            );
        } catch (Exception e) {
            log.warn("Falha ao parsear payload Kafka: {}", payload);
            return null;
        }
    }
}
