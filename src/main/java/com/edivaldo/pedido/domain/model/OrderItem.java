package com.edivaldo.pedido.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItem {

    private UUID id;
    private String productId;
    private int quantity;
    private BigDecimal unitPrice;

    public OrderItem(String productId, int quantity, BigDecimal unitPrice) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Preco unitario invalido");

        this.id = UUID.randomUUID();
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // Construtor usado pela camada de persistencia para reconstruir o objeto
    public OrderItem(UUID id, String productId, int quantity, BigDecimal unitPrice) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public UUID getId()          { return id; }
    public String getProductId() { return productId; }
    public int getQuantity()     { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
