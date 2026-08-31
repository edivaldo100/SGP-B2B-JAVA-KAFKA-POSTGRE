package com.edivaldo.pedido.infrastructure.messaging;

public record KafkaConfirmedEvent(String topic, String payload) {}
