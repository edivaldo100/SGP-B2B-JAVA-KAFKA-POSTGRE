package com.edivaldo.pedido.infrastructure.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePartnerRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String name,

    @DecimalMin(value = "0.01", message = "Limite de crédito deve ser maior que zero")
    BigDecimal creditLimit
) {}
