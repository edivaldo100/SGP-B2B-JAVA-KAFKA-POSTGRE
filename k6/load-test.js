import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// ─── Métricas customizadas ────────────────────────────────────────────────────
const orderCreatedOk   = new Counter('orders_created_ok');
const orderCreatedFail = new Counter('orders_created_fail');
const idempotencyHit   = new Counter('idempotency_cache_hit');
const creditDenied     = new Counter('credit_denied');
const orderCreateDuration = new Trend('order_create_duration', true);
const errorRate = new Rate('error_rate');

// ─── Configuração do cenário ──────────────────────────────────────────────────
export const options = {
  scenarios: {
    // Rampa de carga: simula crescimento orgânico de tráfego
    ramp_up: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 100 },  // aquecimento
        { duration: '1m',  target: 300 },  // carga normal
        { duration: '1m',  target: 600 },  // pico
        { duration: '30s', target: 300 },  // volta ao normal
        { duration: '30s', target: 0   },  // resfriamento
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_duration:     ['p(95)<800', 'p(99)<1500'],  // Docker Desktop local: margem maior
    // http_req_failed conta 422 (credito insuficiente) como falha — excluido intencionalmente
    // usar error_rate (checks que falharam) como indicador real de erro
    error_rate:            ['rate<0.05'],
    order_create_duration: ['p(95)<1000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:80';

const PRODUCTS = ['PROD-001', 'PROD-002', 'PROD-003', 'PROD-004', 'PROD-005'];

// ─── Setup: cria parceiros e retorna UUIDs reais ──────────────────────────────
export function setup() {
  console.log(`Iniciando teste contra: ${BASE_URL}`);
  const health = http.get(`${BASE_URL}/actuator/health`);
  if (health.status !== 200) {
    throw new Error(`Aplicacao nao esta saudavel: ${health.status}`);
  }
  console.log('Health check OK');

  const partners = [];
  const runId = Date.now();
  for (let i = 1; i <= 5; i++) {
    const res = http.post(
      `${BASE_URL}/api/v1/partners`,
      JSON.stringify({ name: `K6 Partner ${runId}-${i}`, creditLimit: 10000000 }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    if (res.status === 201 || res.status === 200) {
      partners.push(res.json().partnerUuid);
    } else {
      console.warn(`Falha ao criar parceiro ${i}: status=${res.status} body=${res.body}`);
    }
  }

  if (partners.length === 0) {
    throw new Error('Nenhum parceiro criado — abortando teste');
  }
  console.log(`Parceiros criados: ${partners.length}`);
  return { partners };
}

// ─── Cenário principal ────────────────────────────────────────────────────────
export default function (data) {
  const partnerId = data.partners[Math.floor(Math.random() * data.partners.length)];
  const idempotencyKey = uuidv4();

  const payload = buildPayload();

  // 1. Criar pedido
  const startTime = Date.now();
  const createRes = http.post(
    `${BASE_URL}/api/v1/orders`,
    JSON.stringify(payload),
    {
      headers: {
        'Content-Type':   'application/json',
        'X-Partner-Id':   partnerId,
        'Idempotency-Key': idempotencyKey,
      },
      tags: { endpoint: 'create_order' },
    }
  );
  orderCreateDuration.add(Date.now() - startTime);

  const createOk = check(createRes, {
    'criar pedido: status 201 ou 409':  (r) => [201, 409, 422].includes(r.status),
    'criar pedido: tem body':           (r) => r.body && r.body.length > 0,
  });

  errorRate.add(!createOk);

  if (createRes.status === 201) {
    orderCreatedOk.add(1);
    const order = createRes.json();

    sleep(0.1);

    // 2. Testar idempotência — mesma key, mesmo payload → deve retornar 201 cacheado
    const idempotentRes = http.post(
      `${BASE_URL}/api/v1/orders`,
      JSON.stringify(payload),
      {
        headers: {
          'Content-Type':   'application/json',
          'X-Partner-Id':   partnerId,
          'Idempotency-Key': idempotencyKey,
        },
        tags: { endpoint: 'create_order_idempotent' },
      }
    );

    check(idempotentRes, {
      'idempotencia: retorna 201 ou 409': (r) => [201, 409].includes(r.status),
    });

    if (idempotentRes.status === 201 || idempotentRes.status === 409) {
      idempotencyHit.add(1);
    }

    sleep(0.1);

    // 3. Buscar o pedido criado
    const getRes = http.get(
      `${BASE_URL}/api/v1/orders/${order.id}`,
      { tags: { endpoint: 'get_order' } }
    );

    check(getRes, {
      'buscar pedido: status 200': (r) => r.status === 200,
      'buscar pedido: status correto': (r) => {
        const body = r.json();
        return body && body.status === 'PENDENTE';
      },
    });

    sleep(0.2);

    // 4. Aprovar o pedido (30% das vezes)
    if (Math.random() < 0.3) {
      const approveRes = http.patch(
        `${BASE_URL}/api/v1/orders/${order.id}/status`,
        JSON.stringify({ status: 'APROVADO' }),
        {
          headers: { 'Content-Type': 'application/json' },
          tags: { endpoint: 'approve_order' },
        }
      );

      check(approveRes, {
        'aprovar pedido: status 200 ou 422': (r) => [200, 422].includes(r.status),
      });
    }

  } else if (createRes.status === 422) {
    creditDenied.add(1);
  } else {
    orderCreatedFail.add(1);
  }

  sleep(randomBetween(0.5, 1.5));
}

// ─── Helpers ─────────────────────────────────────────────────────────────────
function buildPayload() {
  const itemCount = Math.floor(Math.random() * 3) + 1;
  const items = [];
  for (let i = 0; i < itemCount; i++) {
    items.push({
      productId: PRODUCTS[Math.floor(Math.random() * PRODUCTS.length)],
      quantity: Math.floor(Math.random() * 5) + 1,
      unitPrice: (Math.random() * 500 + 10).toFixed(2),
    });
  }
  return { items };
}

function randomBetween(min, max) {
  return Math.random() * (max - min) + min;
}
