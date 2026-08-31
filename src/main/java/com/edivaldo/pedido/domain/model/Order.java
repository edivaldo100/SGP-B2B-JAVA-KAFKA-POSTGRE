package com.edivaldo.pedido.domain.model;

import com.edivaldo.pedido.domain.exception.InvalidStatusTransitionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Order {

    private UUID id;
    private UUID partnerId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Factory — cria um pedido novo
    public static Order create(UUID partnerId, List<OrderItem> items) {
        Order order = new Order();
        order.id = UUID.randomUUID();
        order.partnerId = partnerId;
        order.items = new ArrayList<>(items);
        order.totalAmount = order.calculateTotal();
        order.status = OrderStatus.PENDENTE;
        order.createdAt = LocalDateTime.now();
        order.updatedAt = LocalDateTime.now();
        return order;
    }

    // Construtor usado pela persistencia para reconstruir o agregado
    public static Order reconstitute(UUID id, UUID partnerId, List<OrderItem> items,
                                     BigDecimal totalAmount, OrderStatus status,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        Order order = new Order();
        order.id = id;
        order.partnerId = partnerId;
        order.items = new ArrayList<>(items);
        order.totalAmount = totalAmount;
        order.status = status;
        order.createdAt = createdAt;
        order.updatedAt = updatedAt;
        return order;
    }

    private Order() {}

    // ─── Regras de negócio ────────────────────────────────────────────────────

    public void approve() {
        validateTransition(OrderStatus.APROVADO);
        this.status = OrderStatus.APROVADO;
        this.updatedAt = LocalDateTime.now();
    }

    public void startProcessing() {
        validateTransition(OrderStatus.EM_PROCESSAMENTO);
        this.status = OrderStatus.EM_PROCESSAMENTO;
        this.updatedAt = LocalDateTime.now();
    }

    public void ship() {
        validateTransition(OrderStatus.ENVIADO);
        this.status = OrderStatus.ENVIADO;
        this.updatedAt = LocalDateTime.now();
    }

    public void deliver() {
        validateTransition(OrderStatus.ENTREGUE);
        this.status = OrderStatus.ENTREGUE;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (status == OrderStatus.ENTREGUE || status == OrderStatus.CANCELADO) {
            throw new InvalidStatusTransitionException(
                "Pedido com status " + status + " nao pode ser cancelado");
        }
        this.status = OrderStatus.CANCELADO;
        this.updatedAt = LocalDateTime.now();
    }

    // Retorna true se o crédito já foi debitado (ocorre na transição para APROVADO).
    // Usado para decidir se um cancelamento deve estornar crédito.
    public boolean isCreditDebitable() {
        return status == OrderStatus.APROVADO
            || status == OrderStatus.EM_PROCESSAMENTO
            || status == OrderStatus.ENVIADO;
    }

    private void validateTransition(OrderStatus target) {
        boolean valid = switch (target) {
            case APROVADO         -> status == OrderStatus.PENDENTE;
            case EM_PROCESSAMENTO -> status == OrderStatus.APROVADO;
            case ENVIADO          -> status == OrderStatus.EM_PROCESSAMENTO;
            case ENTREGUE         -> status == OrderStatus.ENVIADO;
            default               -> false;
        };
        if (!valid) {
            throw new InvalidStatusTransitionException(
                "Transicao invalida: " + status + " -> " + target);
        }
    }

    private BigDecimal calculateTotal() {
        return items.stream()
            .map(OrderItem::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public UUID getId()               { return id; }
    public UUID getPartnerId()        { return partnerId; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public BigDecimal getTotalAmount(){ return totalAmount; }
    public OrderStatus getStatus()    { return status; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public LocalDateTime getUpdatedAt(){ return updatedAt; }
}
