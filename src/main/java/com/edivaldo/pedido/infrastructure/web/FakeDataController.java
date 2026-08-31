package com.edivaldo.pedido.infrastructure.web;

import com.edivaldo.pedido.application.service.FakeDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/partners-fakes")
@RequiredArgsConstructor
@Tag(name = "Fake Data", description = "Geração de dados de demonstração")
public class FakeDataController {

    private final FakeDataService fakeDataService;

    @PostMapping
    @Operation(summary = "Gera parceiros e pedidos de demonstração",
               description = "Cria 5 parceiros e ~12 pedidos com status variados para facilitar testes. " +
                             "Parceiros já existentes são ignorados.")
    public ResponseEntity<Map<String, Object>> generate() {
        FakeDataService.FakeDataResult result = fakeDataService.generate();
        return ResponseEntity.ok(Map.of(
            "message", "Dados de demonstração gerados com sucesso",
            "parceiros_criados", result.parceiros(),
            "pedidos_criados", result.pedidos()
        ));
    }
}
