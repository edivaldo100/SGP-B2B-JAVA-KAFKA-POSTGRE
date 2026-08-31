# SGP-B2B — Sistema de Gestão de Pedidos B2B

Microserviço crítico para gestão de pedidos B2B com suporte a alta concorrência, idempotência, controle de crédito e dashboard em tempo real.

**Stack:** Java 21 · Spring Boot 3.3.6 · Virtual Threads · PostgreSQL 16 · Apache Kafka 3.8 (KRaft) · Nginx · React 18 + Vite 5 · Docker Compose

---

## Pré-requisitos

- Docker Desktop

Só. Nenhuma outra dependência local necessária.

---

## Subindo tudo com um único comando

```sh
docker-compose up --build -d
```

Para acompanhar os logs:
```sh
docker-compose logs -f app
```

Para parar e remover volumes:
```sh
docker-compose down --volumes --remove-orphans
```

---

## URLs disponíveis

| Serviço | URL |
|---|---|
| Dashboard (React) | http://localhost/dashboard/ |
| Swagger UI | http://localhost/swagger-ui/index.html |
| API Health | http://localhost/actuator/health |
| Grafana (métricas K6) | http://localhost/grafana/ |
| InfluxDB | http://localhost:8086 |

---

## Exemplos de uso (curl)

### Health Check

```sh
curl http://localhost/actuator/health
```

```json
{ "status": "UP" }
```

---

### Dados de demonstração

Cria 5 parceiros e ~15 pedidos com todos os status (PENDENTE, APROVADO, EM_PROCESSAMENTO, CANCELADO):

```sh
curl -X POST http://localhost/api/v1/partners-fakes
```

```json
{
  "message": "Dados de demonstração gerados com sucesso",
  "parceiros_criados": 5,
  "pedidos_criados": 15
}
```

---

### Parceiros

**Listar parceiros:**
```sh
curl http://localhost/api/v1/partners
```

```json
[
  {
    "id": 1,
    "partnerUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "name": "TechCorp Distribuidora",
    "createdAt": "2026-08-31T10:00:00"
  }
]
```

**Cadastrar parceiro:**
```sh
curl -X POST http://localhost/api/v1/partners \
  -H "Content-Type: application/json" \
  -d '{"name": "Empresa XYZ"}'
```

```json
{
  "id": 6,
  "partnerUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Empresa XYZ",
  "createdAt": "2026-08-31T10:00:00"
}
```

---

### Pedidos

**Criar pedido** (requer UUID do parceiro e Idempotency-Key único):
```sh
curl -X POST http://localhost/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "X-Partner-Id: {partnerUuid}" \
  -H "Idempotency-Key: pedido-001" \
  -d '{
    "items": [
      { "productId": "NOTEBOOK-PRO", "quantity": 2, "unitPrice": 2499.90 },
      { "productId": "MOUSE-GAMER",  "quantity": 5, "unitPrice": 149.90 }
    ]
  }'
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "partnerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "partnerSequentialId": 1,
  "partnerName": "TechCorp Distribuidora",
  "items": [
    {
      "id": "...",
      "productId": "NOTEBOOK-PRO",
      "quantity": 2,
      "unitPrice": 2499.90,
      "subtotal": 4999.80
    }
  ],
  "totalAmount": 5748.30,
  "status": "PENDENTE",
  "createdAt": "2026-08-31T10:00:00",
  "updatedAt": "2026-08-31T10:00:00"
}
```

**Listar todos os pedidos:**
```sh
curl http://localhost/api/v1/orders
```

**Filtrar por parceiro (ID sequencial):**
```sh
curl "http://localhost/api/v1/orders?partnerId=1"
```

**Filtrar por nome do parceiro:**
```sh
curl "http://localhost/api/v1/orders?name=TechCorp"
```

**Filtrar por status:**
```sh
curl "http://localhost/api/v1/orders?status=APROVADO"
```

**Filtrar combinando parceiro e status:**
```sh
curl "http://localhost/api/v1/orders?partnerId=1&status=PENDENTE"
```

**Buscar pedido por ID:**
```sh
curl http://localhost/api/v1/orders/{orderId}
```

**Atualizar status do pedido:**
```sh
curl -X PATCH http://localhost/api/v1/orders/{orderId}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "APROVADO"}'
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "partnerName": "TechCorp Distribuidora",
  "totalAmount": 5748.30,
  "status": "APROVADO",
  "createdAt": "2026-08-31T10:00:00",
  "updatedAt": "2026-08-31T10:01:00"
}
```

Status válidos (máquina de estados):
```
PENDENTE → APROVADO → EM_PROCESSAMENTO → ENVIADO → ENTREGUE
* → CANCELADO  (qualquer status — estorna crédito automaticamente)
```

**Cancelar pedido:**
```sh
curl -X DELETE http://localhost/api/v1/orders/{orderId}
```

**Stream em tempo real (SSE) — atualiza ao criar ou mudar status:**
```sh
curl -N -H "Accept: text/event-stream" http://localhost/api/v1/orders/stream
```

```
event: order
data: {"id":"550e8400...","partnerName":"TechCorp","status":"APROVADO","totalAmount":5748.30,...}

event: order
data: {"id":"660e9500...","partnerName":"InfoShop","status":"PENDENTE","totalAmount":1234.50,...}
```

---

## Teste de carga K6

**Windows:**
```sh
$env:K6_SCRIPT_FILE='test.js'; docker-compose run --rm k6_tester
```

**Linux / macOS:**
```sh
K6_SCRIPT_FILE=test.js docker-compose run --rm k6_tester
```

Resultados disponíveis no Grafana: http://localhost/grafana/d/ffwswyewfdse8b/k6-load-testing-results

---

## Arquitetura

### Visão geral

```
Parceiro / Browser / K6
        │
        ▼ :80
  Nginx (API Gateway)
   ├── /api/          → Orders + Partners API (app:8080)
   ├── /dashboard/    → React Dashboard (dashboard:80)
   └── /grafana/      → Grafana (grafana:3000)
        │
        ▼
   Orders + Partners API (Java 21 · Spring Boot)
   ├── Hexagonal Architecture (Ports & Adapters)
   ├── Virtual Threads
   ├── Idempotência (SHA-256 + TTL 30s)
   ├── Pessimistic Lock (SELECT FOR UPDATE)
   ├── Outbox Pattern (SKIP LOCKED · backoff exponencial)
   └── SSE (Server-Sent Events em tempo real)
        │
        ├── PostgreSQL 16
        │    orders · order_items · partners
        │    partner_credit · idempotency_keys · outbox_events
        │
        └── Apache Kafka 3.8 (KRaft)
             order.events.*
```

Diagramas PlantUML em [`docs/`](docs/):

| Arquivo | Conteúdo |
|---|---|
| `overview.puml` | Visão simplificada do sistema |
| `macro-architecture.puml` | Macro arquitetura por camadas |
| `architecture.puml` | Arquitetura hexagonal completa |
| `api-integration.puml` | Fluxos de integração e SSE |

---

## Funcionalidades

- **Gestão de parceiros** — cadastro com ID sequencial (humano) + UUID (sistema)
- **Gestão de pedidos** — criação com controle de crédito, histórico e filtros
- **Idempotência** — garante que reenvios da mesma requisição não criam duplicatas
- **Controle de crédito** — `SELECT FOR UPDATE` garante consistência sob concorrência
- **Outbox Pattern** — publicação garantida no Kafka mesmo sob falhas
- **SSE** — novos pedidos aparecem no dashboard em tempo real sem polling
- **Dashboard React** — histórico de pedidos ao vivo + métricas Grafana
- **Teste de carga** — K6 → InfluxDB → Grafana
