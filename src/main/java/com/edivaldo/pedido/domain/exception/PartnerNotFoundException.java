package com.edivaldo.pedido.domain.exception;

import java.util.UUID;

public class PartnerNotFoundException extends RuntimeException {
    public PartnerNotFoundException(UUID partnerId) {
        super("Parceiro nao encontrado: " + partnerId);
    }

    public PartnerNotFoundException(String message) {
        super(message);
    }
}
