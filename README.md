# SGP-B2B — Sistema de Gestão de Pedidos B2B

Microserviço crítico para gestão de pedidos B2B com suporte a alta concorrência, idempotência, controle de crédito e dashboard em tempo real.

**Stack:** Java 21 · Spring Boot 3.3.6 · Virtual Threads · PostgreSQL 16 · Apache Kafka 3.8 (KRaft) · Nginx · React 18 + Vite 5 · Docker Compose

---

## Pré-requisitos

- Docker Desktop

Só. Nenhuma outra dependência local necessária.

---

## Subindo tudo com um único comando

```bash
docker compose up --build -d
```

Acompanhar logs da API:

```bash
docker compose logs -f app
```

Parar e remover volumes:

```bash
docker compose down --volumes --remove-orphans
```

---

## URLs disponíveis

| Serviço               | URL                                    |
| --------------------- | -------------------------------------- |
| Dashboard (React)     | http://localhost/dashboard/            |
| Swagger UI            | http://localhost/swagger-ui/index.html |
| API Health            | http://localhost/actuator/health       |
| Grafana (métricas K6) | http://localhost/grafana/              |
| InfluxDB              | http://localhost:8086                  |

O dashboard possui **4 abas**:

- **Histórico de Pedidos** — tabela com SSE em tempo real + filtros por parceiro/status
- **Eventos Kafka** — últimos 20 eventos confirmados pelo Kafka (ordenados do mais recente)
- **Gerenciamento** — CRUD de parceiros e gerenciamento de pedidos
- **Métricas (Grafana)** — dashboard K6 embutido

---

## Exemplos de uso (curl)

### Health Check

```bash
curl http://localhost/actuator/health
```

```json
{ "status": "UP" }
```

---

### Dados de demonstração

Cria 5 parceiros e ~15 pedidos com todos os status:

```bash
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

```bash
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

**Cadastrar parceiro (crédito padrão R$ 100.000.000):**

```bash
curl -X POST http://localhost/api/v1/partners \
  -H "Content-Type: application/json" \
  -d '{"name": "Empresa XYZ"}'
```

**Cadastrar parceiro com limite de crédito personalizado:**

```bash
curl -X POST http://localhost/api/v1/partners \
  -H "Content-Type: application/json" \
  -d '{"name": "Startup ABC", "creditLimit": 50000.00}'
```

```json
{
  "id": 6,
  "partnerUuid": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "name": "Startup ABC",
  "createdAt": "2026-08-31T10:00:00"
}
```

**Remover parceiro:**

```bash
curl -X DELETE http://localhost/api/v1/partners/6
```

`HTTP 204 No Content`

**Extrato de movimentações de crédito (paginado):**

```bash
curl "http://localhost/api/v1/partners/{partnerUuid}/credit-transactions?page=0&size=20"
```

```json
{
  "content": [
    {
      "id": "b398d6c3-...",
      "partnerId": "3fa85f64-...",
      "orderId": "550e8400-...",
      "type": "RELEASE",
      "amount": 1000.00,
      "createdAt": "2026-08-31T10:02:00"
    },
    {
      "id": "94fa43f6-...",
      "partnerId": "3fa85f64-...",
      "orderId": "550e8400-...",
      "type": "DEBIT",
      "amount": 1000.00,
      "createdAt": "2026-08-31T10:01:00"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

- `DEBIT` — crédito debitado ao aprovar o pedido (`PENDENTE → APROVADO`)
- `RELEASE` — crédito estornado ao cancelar um pedido já aprovado

> A fonte de verdade do saldo atual é `partner_credit.available_credit`. A tabela `credit_transaction` é auditoria imutável (append-only).

---

### Pedidos

**Criar pedido** (requer UUID do parceiro e Idempotency-Key único):

```bash
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
      "unitPrice": 2499.9,
      "subtotal": 4999.8
    },
    {
      "id": "...",
      "productId": "MOUSE-GAMER",
      "quantity": 5,
      "unitPrice": 149.9,
      "subtotal": 749.5
    }
  ],
  "totalAmount": 5749.3,
  "status": "PENDENTE",
  "createdAt": "2026-08-31T10:00:00",
  "updatedAt": "2026-08-31T10:00:00"
}
```

**Listar todos os pedidos:**

```bash
curl http://localhost/api/v1/orders
```

**Filtrar por parceiro (ID sequencial):**

```bash
curl "http://localhost/api/v1/orders?partnerId=1"
```

**Filtrar por nome do parceiro:**

```bash
curl "http://localhost/api/v1/orders?name=TechCorp"
```

**Filtrar por status:**

```bash
curl "http://localhost/api/v1/orders?status=APROVADO"
```

**Filtrar combinando parceiro e status:**

```bash
curl "http://localhost/api/v1/orders?partnerId=1&status=PENDENTE"
```

**Buscar pedido por ID:**

```bash
curl http://localhost/api/v1/orders/{orderId}
```

**Atualizar status do pedido:**

```bash
curl -X PATCH http://localhost/api/v1/orders/{orderId}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "APROVADO"}'
```

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "partnerName": "TechCorp Distribuidora",
  "totalAmount": 5749.3,
  "status": "APROVADO",
  "createdAt": "2026-08-31T10:00:00",
  "updatedAt": "2026-08-31T10:01:00"
}
```

Status válidos:

```
PENDENTE → APROVADO → EM_PROCESSAMENTO → ENVIADO → ENTREGUE
* → CANCELADO  (qualquer status — estorna crédito automaticamente)
```

Enviar um valor de status inválido retorna **HTTP 400**:

```bash
curl -X PATCH http://localhost/api/v1/orders/{orderId}/status \
  -H "Content-Type: application/json" \
  -d '{"status": "INVALIDO"}'
```

```json
{
  "status": 400,
  "detail": "Requisição inválida: Cannot deserialize value of type `OrderStatus` from String \"INVALIDO\"...",
  "timestamp": "2026-08-31T10:00:00Z"
}
```

**Cancelar pedido:**

```bash
curl -X DELETE http://localhost/api/v1/orders/{orderId}
```

**Stream de pedidos em tempo real (SSE):**

```bash
curl -N -H "Accept: text/event-stream" http://localhost/api/v1/orders/stream
```

```
event: order
data: {"id":"550e8400...","partnerName":"TechCorp","status":"APROVADO","totalAmount":5749.30,...}
```

**Stream de eventos Kafka confirmados (SSE — últimos 20):**

```bash
curl -N -H "Accept: text/event-stream" http://localhost/api/v1/orders/kafka/stream
```

```
event: kafka
data: {"topic":"order.events.order.created","orderId":"550e8400...","partnerId":"3fa85f64...","status":"PENDENTE","receivedAt":"2026-08-31T10:00:00Z"}

event: kafka
data: {"topic":"order.events.order.approved","orderId":"550e8400...","partnerId":"3fa85f64...","status":"APROVADO","receivedAt":"2026-08-31T10:01:00Z"}
```

---

## Teste de carga K6

Script padrão (`k6/load-test.js`):

```bash
docker compose --profile k6 run --rm k6_tester
```

Script customizado (ex: `testes/test.js`):

**Windows (PowerShell):**

```bash
$env:K6_SCRIPT_FILE='/testes/test.js'; docker compose --profile k6 run --rm k6_tester
```

**Linux / macOS / Git Bash:**

```bash
K6_SCRIPT_FILE=/testes/test.js docker compose --profile k6 run --rm k6_tester
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
   ├── Idempotência (SHA-256 + TTL 30min · reclaimExpired atômico para chaves expiradas)
   ├── Pessimistic Lock (SELECT FOR UPDATE)
   ├── Outbox Pattern (SKIP LOCKED · backoff exponencial)
   ├── SSE pedidos em tempo real  (/api/v1/orders/stream)
   └── SSE eventos Kafka          (/api/v1/orders/kafka/stream)
        │
        ├── PostgreSQL 16
        │    orders · order_items · partners
        │    partner_credit · idempotency_keys · outbox_events
        │    credit_transaction (auditoria append-only)
        │
        └── Apache Kafka 3.8 (KRaft)
             order.events.*
             └── OutboxPublisher → KafkaConfirmedEvent → KafkaSseService → Browser
```

### Visão geral do sistema

![SGP-B2B Overview](docs/SGP-B2B%20Overview.svg)

### Arquitetura hexagonal

![SGP-B2B Architecture](docs/SGP-B2B%20Architecture.svg)

### Fluxos de integração e SSE

![SGP-B2B API Integration](docs/SGP-B2B%20API%20Integration.svg)

Fontes PlantUML em [`docs/`](docs/):

| Arquivo                   | Conteúdo                       |
| ------------------------- | ------------------------------ |
| `overview.puml`           | Visão simplificada do sistema  |
| `macro-architecture.puml` | Macro arquitetura por camadas  |
| `architecture.puml`       | Arquitetura hexagonal completa |
| `api-integration.puml`    | Fluxos de integração e SSE     |

---
