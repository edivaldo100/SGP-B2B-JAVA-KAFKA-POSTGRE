package com.edivaldo.pedido.application.service;

import com.edivaldo.pedido.application.command.CreateOrderCommand;
import com.edivaldo.pedido.application.command.UpdateOrderStatusCommand;
import com.edivaldo.pedido.application.dto.OrderResponse;
import com.edivaldo.pedido.application.dto.PartnerResponse;
import com.edivaldo.pedido.domain.model.OrderStatus;
import com.edivaldo.pedido.domain.port.in.CreateOrderUseCase;
import com.edivaldo.pedido.domain.port.in.UpdateOrderStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FakeDataService {

    private static final String[][] PRODUTOS = {
        {"NOTEBOOK-PRO",  "2", "2499.90"},
        {"MOUSE-GAMER",   "5", "149.90"},
        {"TECLADO-MECH",  "2", "389.00"},
        {"MONITOR-4K",    "1", "1799.00"},
        {"SSD-1TB",       "4", "329.90"},
        {"HEADSET-USB",   "3", "219.90"},
        {"WEBCAM-HD",     "6", "189.00"},
        {"HUB-USB-C",     "8", "89.90"},
        {"CADEIRA-EXEC",  "1", "1199.00"},
        {"IMPRESSORA-A4", "2", "599.90"},
        {"ROTEADOR-WIFI", "3", "299.90"},
        {"SWITCH-24P",    "1", "899.00"},
    };

    private static final String[] NOMES_PARCEIROS = {
        "TechCorp Distribuidora",
        "InfoShop Brasil",
        "MegaStore Eletrônicos",
        "RapidBuy Marketplace",
        "DataLink Solutions",
    };

    // Sequência de status para cada pedido (PENDENTE é o estado inicial, sem transição)
    // null = deixar PENDENTE
    private static final OrderStatus[] SEQUENCIA_STATUS = {
        null,                       // pedido 1 → PENDENTE
        OrderStatus.APROVADO,       // pedido 2 → APROVADO
        OrderStatus.CANCELADO,      // pedido 3 → CANCELADO
        OrderStatus.APROVADO,       // pedido 4 → APROVADO (depois EM_PROCESSAMENTO)
        null,                       // pedido 5 → PENDENTE
        OrderStatus.CANCELADO,      // pedido 6 → CANCELADO
    };

    // Segundo passo de transição (aplicado sobre o primeiro)
    private static final OrderStatus[] SEGUNDA_TRANSICAO = {
        null,
        null,
        null,
        OrderStatus.EM_PROCESSAMENTO, // APROVADO → EM_PROCESSAMENTO
        null,
        null,
    };

    private final PartnerService partnerService;
    private final CreateOrderUseCase createOrderUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

    public FakeDataResult generate() {
        List<PartnerResponse> parceiros = criarParceiros();
        List<OrderResponse> pedidos = criarPedidos(parceiros);
        log.info("Fake data gerado: {} parceiros, {} pedidos", parceiros.size(), pedidos.size());
        return new FakeDataResult(parceiros.size(), pedidos.size());
    }

    private List<PartnerResponse> criarParceiros() {
        List<PartnerResponse> lista = new ArrayList<>();
        for (String nome : NOMES_PARCEIROS) {
            try {
                lista.add(partnerService.create(nome));
            } catch (IllegalArgumentException e) {
                partnerService.findAll().stream()
                    .filter(p -> p.name().equals(nome))
                    .findFirst()
                    .ifPresent(lista::add);
            }
        }
        return lista;
    }

    private List<OrderResponse> criarPedidos(List<PartnerResponse> parceiros) {
        List<OrderResponse> lista = new ArrayList<>();
        int prodIdx = 0;
        int seqIdx = 0;

        for (PartnerResponse parceiro : parceiros) {
            // Cada parceiro recebe pedidos que cobrem todos os status ao longo do conjunto
            int qtdPedidos = SEQUENCIA_STATUS.length / parceiros.size() + 2;

            for (int i = 0; i < qtdPedidos; i++) {
                String[] prod1 = PRODUTOS[prodIdx % PRODUTOS.length];
                String[] prod2 = PRODUTOS[(prodIdx + 3) % PRODUTOS.length];
                prodIdx++;

                List<CreateOrderCommand.Item> itens = List.of(
                    new CreateOrderCommand.Item(prod1[0], Integer.parseInt(prod1[1]), new BigDecimal(prod1[2])),
                    new CreateOrderCommand.Item(prod2[0], Integer.parseInt(prod2[1]), new BigDecimal(prod2[2]))
                );

                String idemKey = "fake-" + parceiro.id() + "-pedido-" + i;
                String hash    = String.valueOf(idemKey.hashCode());

                try {
                    OrderResponse pedido = createOrderUseCase.execute(
                        new CreateOrderCommand(parceiro.partnerUuid(), idemKey, hash, itens)
                    );
                    lista.add(pedido);

                    // Primeira transição
                    OrderStatus primeiraTransicao = SEQUENCIA_STATUS[seqIdx % SEQUENCIA_STATUS.length];
                    OrderStatus segundaTransicao  = SEGUNDA_TRANSICAO[seqIdx % SEGUNDA_TRANSICAO.length];
                    seqIdx++;

                    if (primeiraTransicao != null) {
                        pedido = updateStatus(pedido.id(), primeiraTransicao);
                    }
                    if (pedido != null && segundaTransicao != null) {
                        updateStatus(pedido.id(), segundaTransicao);
                    }

                } catch (Exception e) {
                    log.warn("Erro ao criar pedido fake: {}", e.getMessage());
                }
            }
        }
        return lista;
    }

    private OrderResponse updateStatus(java.util.UUID orderId, OrderStatus status) {
        try {
            return updateOrderStatusUseCase.execute(new UpdateOrderStatusCommand(orderId, status));
        } catch (Exception e) {
            log.warn("Não foi possível atualizar status {} para pedido {}: {}", status, orderId, e.getMessage());
            return null;
        }
    }

    public record FakeDataResult(int parceiros, int pedidos) {}
}
