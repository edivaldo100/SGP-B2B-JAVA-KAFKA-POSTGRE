package com.edivaldo.pedido.infrastructure.web;

import com.edivaldo.pedido.application.dto.CreditTransactionResponse;
import com.edivaldo.pedido.application.dto.PartnerResponse;
import com.edivaldo.pedido.application.service.PartnerService;
import com.edivaldo.pedido.domain.port.in.GetCreditTransactionsUseCase;
import com.edivaldo.pedido.infrastructure.web.dto.CreatePartnerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
@Tag(name = "Partners", description = "Cadastro de parceiros B2B")
public class PartnerController {

    private final PartnerService partnerService;
    private final GetCreditTransactionsUseCase getCreditTransactionsUseCase;

    @PostMapping
    @Operation(summary = "Cadastra um novo parceiro")
    public ResponseEntity<PartnerResponse> create(@Valid @RequestBody CreatePartnerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(partnerService.create(request.name(), request.creditLimit()));
    }

    @GetMapping
    @Operation(summary = "Lista todos os parceiros")
    public ResponseEntity<List<PartnerResponse>> findAll() {
        return ResponseEntity.ok(partnerService.findAll());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um parceiro")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        partnerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{partnerUuid}/credit-transactions")
    @Operation(summary = "Extrato de movimentações de crédito do parceiro (paginado)")
    public ResponseEntity<Page<CreditTransactionResponse>> getCreditTransactions(
            @PathVariable UUID partnerUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(getCreditTransactionsUseCase.execute(partnerUuid, page, size));
    }
}
