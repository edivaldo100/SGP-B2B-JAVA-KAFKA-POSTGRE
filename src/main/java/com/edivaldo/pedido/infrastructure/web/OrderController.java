package com.edivaldo.pedido.infrastructure.web;

import com.edivaldo.pedido.application.command.CreateOrderCommand;
import com.edivaldo.pedido.application.command.UpdateOrderStatusCommand;
import com.edivaldo.pedido.application.dto.OrderResponse;
import com.edivaldo.pedido.domain.model.OrderStatus;
import com.edivaldo.pedido.domain.port.in.CancelOrderUseCase;
import com.edivaldo.pedido.domain.port.in.CreateOrderUseCase;
import com.edivaldo.pedido.domain.port.in.FindOrderUseCase;
import com.edivaldo.pedido.domain.port.in.UpdateOrderStatusUseCase;
import com.edivaldo.pedido.infrastructure.web.dto.CreateOrderRequest;
import com.edivaldo.pedido.infrastructure.web.dto.UpdateOrderStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Gerenciamento de pedidos B2B")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final FindOrderUseCase findOrderUseCase;

    @PostMapping
    @Operation(summary = "Cria um novo pedido")
    public ResponseEntity<OrderResponse> create(
            @RequestHeader("X-Partner-Id") UUID partnerId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {

        String requestHash = sha256(request.toString());

        List<CreateOrderCommand.Item> items = request.items().stream()
                .map(i -> new CreateOrderCommand.Item(i.productId(), i.quantity(), i.unitPrice()))
                .toList();

        CreateOrderCommand command = new CreateOrderCommand(partnerId, idempotencyKey, requestHash, items);
        OrderResponse response = createOrderUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Atualiza o status de um pedido")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        UpdateOrderStatusCommand command = new UpdateOrderStatusCommand(orderId, request.status());
        return ResponseEntity.ok(updateOrderStatusUseCase.execute(command));
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancela um pedido")
    public ResponseEntity<OrderResponse> cancel(@PathVariable UUID orderId) {
        return ResponseEntity.ok(cancelOrderUseCase.execute(orderId));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Busca um pedido pelo ID")
    public ResponseEntity<OrderResponse> findById(@PathVariable UUID orderId) {
        return ResponseEntity.ok(findOrderUseCase.findById(orderId));
    }

    @GetMapping
    @Operation(summary = "Lista pedidos por parceiro ou status")
    public ResponseEntity<List<OrderResponse>> findAll(
            @RequestParam(required = false) UUID partnerId,
            @RequestParam(required = false) OrderStatus status) {

        if (partnerId != null && status != null) {
            return ResponseEntity.ok(findOrderUseCase.findByPartnerId(partnerId).stream()
                    .filter(o -> o.status() == status)
                    .toList());
        }
        if (partnerId != null) {
            return ResponseEntity.ok(findOrderUseCase.findByPartnerId(partnerId));
        }
        if (status != null) {
            return ResponseEntity.ok(findOrderUseCase.findByStatus(status));
        }
        return ResponseEntity.badRequest().build();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nao disponivel", e);
        }
    }
}
