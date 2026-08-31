package com.edivaldo.pedido.domain.exception;

public class DuplicateOperationException extends RuntimeException {
    public DuplicateOperationException(String message) {
        super(message);
    }
}
