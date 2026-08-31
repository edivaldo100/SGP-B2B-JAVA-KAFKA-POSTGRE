package com.edivaldo.pedido.infrastructure.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "Pelo menos um item e obrigatorio")
        @Valid
        List<Item> items
) {
    public record Item(
            @NotNull(message = "productId e obrigatorio")
            String productId,

            @Positive(message = "Quantidade deve ser maior que zero")
            int quantity,

            @NotNull
            @PositiveOrZero(message = "Preco unitario nao pode ser negativo")
            BigDecimal unitPrice
    ) {}
}
