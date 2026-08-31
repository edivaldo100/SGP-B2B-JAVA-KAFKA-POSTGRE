package com.edivaldo.pedido.infrastructure.messaging;

import com.edivaldo.pedido.domain.model.OutboxEvent;
import com.edivaldo.pedido.domain.port.out.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final int BATCH_SIZE = 50;
    private static final String TOPIC_PREFIX = "order.events.";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publish() {
        List<OutboxEvent> events = outboxEventRepository.findPendingForUpdate(BATCH_SIZE);
        if (events.isEmpty()) return;

        log.debug("Processando {} eventos do outbox", events.size());

        for (OutboxEvent event : events) {
            String topic = TOPIC_PREFIX + event.getEventType().toLowerCase().replace("_", ".");
            try {
                // Envio síncrono: bloqueia até Kafka confirmar (ou timeout 5s)
                // markPublished roda na mesma transação — sem chance de re-publicar
                kafkaTemplate.send(topic, event.getAggregateId().toString(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                outboxEventRepository.markPublished(event.getId());
                log.debug("Evento publicado: {} -> {}", event.getId(), topic);
            } catch (Exception e) {
                handleFailure(event, e);
            }
        }
    }

    private void handleFailure(OutboxEvent event, Throwable ex) {
        int nextRetry = event.getRetryCount() + 1;
        log.warn("Falha ao publicar evento {} (tentativa {}): {}", event.getId(), nextRetry, ex.getMessage());
        outboxEventRepository.markFailed(event.getId(), ex.getMessage(), nextRetry);

        if (nextRetry >= OutboxEvent.MAX_RETRIES) {
            log.error("Evento {} excedeu {} tentativas e foi marcado como FAILED.",
                    event.getId(), OutboxEvent.MAX_RETRIES);
        }
    }
}
