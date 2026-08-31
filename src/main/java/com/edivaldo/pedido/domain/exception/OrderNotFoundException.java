package com.edivaldo.pedido.domain.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(UUID id) {
        super("Pedido nao encontrado: " + id);
    }
}
