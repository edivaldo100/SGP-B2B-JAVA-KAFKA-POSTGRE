import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// ─── Stress test: NÃO é o teste de regressão (load-test.js) ──────────────────
// Objetivo único: achar o teto real desta máquina, subindo VUs em degraus até
// a taxa de erro ou a latência estourarem de vez. Sem thresholds bloqueantes —
// o teste roda até o fim mesmo passando do limite, pra gente ver a curva toda.

const orderCreatedOk   = new Counter('orders_created_ok');
const orderCreatedFail = new Counter('orders_created_fail');
const creditDenied     = new Counter('credit_denied');
const orderCreateDuration = new Trend('order_create_duration', true);
const errorRate = new Rate('error_rate');

export const options = {
  scenarios: {
    stress: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 200  },
        { duration: '20s', target: 400  },
        { duration: '20s', target: 700  },
        { duration: '20s', target: 1000 },
        { duration: '20s', target: 1400 },
        { duration: '20s', target: 1800 },
        { duration: '20s', target: 2200 },
        { duration: '30s', target: 2200 }, // sustenta o pico pra ver se estabiliza ou degrada
        { duration: '20s', target: 0    },
      ],
      gracefulRampDown: '15s',
    },
  },
  // sem thresholds que abortam — só queremos observar onde a curva quebra
  thresholds: {
    error_rate: [{ threshold: 'rate<1.0', abortOnFail: false }],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:80';
const PRODUCTS = ['PROD-001', 'PROD-002', 'PROD-003', 'PROD-004', 'PROD-005'];

export function setup() {
  console.log(`Iniciando STRESS TEST contra: ${BASE_URL}`);
  const health = http.get(`${BASE_URL}/actuator/health`);
  if (health.status !== 200) {
    throw new Error(`Aplicacao nao esta saudavel: ${health.status}`);
  }

  const partners = [];
  const runId = Date.now();
  const PARTNER_COUNT = 200; // bem distribuído — objetivo é achar teto de infra, não de lock
  for (let i = 1; i <= PARTNER_COUNT; i++) {
    const res = http.post(
      `${BASE_URL}/api/v1/partners`,
      JSON.stringify({ name: `Stress Partner ${runId}-${i}`, creditLimit: 100000000 }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    if (res.status === 201 || res.status === 200) {
      partners.push(res.json().partnerUuid);
    }
  }

  if (partners.length === 0) {
    throw new Error('Nenhum parceiro criado — abortando teste');
  }
  console.log(`Parceiros criados: ${partners.length}`);
  return { partners };
}

export default function (data) {
  const partnerId = data.partners[Math.floor(Math.random() * data.partners.length)];
  const idempotencyKey = uuidv4();
  const payload = buildPayload();

  const startTime = Date.now();
  const createRes = http.post(
    `${BASE_URL}/api/v1/orders`,
    JSON.stringify(payload),
    {
      headers: {
        'Content-Type':    'application/json',
        'X-Partner-Id':    partnerId,
        'Idempotency-Key': idempotencyKey,
      },
      tags: { endpoint: 'create_order' }, // tag fixa — evita explosão de cardinalidade no InfluxDB
    }
  );
  orderCreateDuration.add(Date.now() - startTime);

  const createOk = check(createRes, {
    'criar pedido: status 201/409/422': (r) => [201, 409, 422].includes(r.status),
  });
  errorRate.add(!createOk);

  if (createRes.status === 201) {
    orderCreatedOk.add(1);
  } else if (createRes.status === 422) {
    creditDenied.add(1);
  } else {
    orderCreatedFail.add(1);
  }

  sleep(randomBetween(0.1, 0.4)); // think time bem menor — queremos pressão máxima
}

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
