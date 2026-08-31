package com.edivaldo.pedido.application.service;

import com.edivaldo.pedido.application.command.CreateOrderCommand;
import com.edivaldo.pedido.application.dto.OrderResponse;
import com.edivaldo.pedido.application.dto.PartnerResponse;
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
        {"NOTEBOOK-PRO",  "3", "2499.90"},
        {"MOUSE-GAMER",   "5", "149.90"},
        {"TECLADO-MECH",  "2", "389.00"},
        {"MONITOR-4K",    "1", "1799.00"},
        {"SSD-1TB",       "4", "329.90"},
        {"HEADSET-USB",   "3", "219.90"},
        {"WEBCAM-HD",     "6", "189.00"},
        {"HUB-USB-C",     "8", "89.90"},
        {"CADEIRA-EXEC",  "1", "1199.00"},
        {"IMPRESSORA-A4", "2", "599.90"},
    };

    private static final String[] NOMES_PARCEIROS = {
        "TechCorp Distribuidora",
        "InfoShop Brasil",
        "MegaStore Eletrônicos",
        "RapidBuy Marketplace",
        "DataLink Solutions",
    };

    private static final String[] STATUS_ATUALIZADOS = {
        "APROVADO", "APROVADO", "APROVADO", "CANCELADO"
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
                log.warn("Parceiro '{}' já existe, pulando", nome);
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

        for (PartnerResponse parceiro : parceiros) {
            int qtdPedidos = 2 + (parceiros.indexOf(parceiro) % 3); // 2, 3 ou 4 pedidos por parceiro
            for (int i = 0; i < qtdPedidos; i++) {
                String[] prod1 = PRODUTOS[prodIdx % PRODUTOS.length];
                String[] prod2 = PRODUTOS[(prodIdx + 1) % PRODUTOS.length];
                prodIdx += 2;

                List<CreateOrderCommand.Item> itens = List.of(
                    new CreateOrderCommand.Item(prod1[0], Integer.parseInt(prod1[1]), new BigDecimal(prod1[2])),
                    new CreateOrderCommand.Item(prod2[0], Integer.parseInt(prod2[1]), new BigDecimal(prod2[2]))
                );

                String idemKey = "fake-" + parceiro.id() + "-" + i;
                String hash = String.valueOf(idemKey.hashCode());

                try {
                    OrderResponse pedido = createOrderUseCase.execute(
                        new CreateOrderCommand(parceiro.partnerUuid(), idemKey, hash, itens)
                    );
                    lista.add(pedido);

                    // Aprova ou cancela alternadamente para ter dados variados
                    String novoStatus = STATUS_ATUALIZADOS[lista.size() % STATUS_ATUALIZADOS.length];
                    try {
                        updateOrderStatusUseCase.execute(
                            new com.edivaldo.pedido.application.command.UpdateOrderStatusCommand(
                                pedido.id(),
                                com.edivaldo.pedido.domain.model.OrderStatus.valueOf(novoStatus)
                            )
                        );
                    } catch (Exception e) {
                        log.warn("Não foi possível atualizar status do pedido {}: {}", pedido.id(), e.getMessage());
                    }
                } catch (Exception e) {
                    log.warn("Erro ao criar pedido fake para parceiro {}: {}", parceiro.name(), e.getMessage());
                }
            }
        }
        return lista;
    }

    public record FakeDataResult(int parceiros, int pedidos) {}
}
