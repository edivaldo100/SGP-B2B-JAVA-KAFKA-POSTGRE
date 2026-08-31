package com.edivaldo.pedido.infrastructure.web;

import com.edivaldo.pedido.application.dto.PartnerResponse;
import com.edivaldo.pedido.application.service.PartnerService;
import com.edivaldo.pedido.infrastructure.web.dto.CreatePartnerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
@Tag(name = "Partners", description = "Cadastro de parceiros B2B")
public class PartnerController {

    private final PartnerService partnerService;

    @PostMapping
    @Operation(summary = "Cadastra um novo parceiro")
    public ResponseEntity<PartnerResponse> create(@Valid @RequestBody CreatePartnerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partnerService.create(request.name()));
    }

    @GetMapping
    @Operation(summary = "Lista todos os parceiros")
    public ResponseEntity<List<PartnerResponse>> findAll() {
        return ResponseEntity.ok(partnerService.findAll());
    }
}
