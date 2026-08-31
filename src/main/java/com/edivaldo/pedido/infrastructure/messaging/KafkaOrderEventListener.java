package com.edivaldo.pedido.infrastructure.messaging;

import com.edivaldo.pedido.infrastructure.web.KafkaSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderEventListener {

    private final KafkaSseService kafkaSseService;

    @KafkaListener(topicPattern = "order\\.events\\..*", groupId = "order-service-sse")
    public void onOrderEvent(
            String payload,
            @Header("kafka_receivedTopic") String topic
    ) {
        log.debug("Evento Kafka recebido: topic={} payload={}", topic, payload);
        kafkaSseService.broadcast(topic, payload);
    }
}
