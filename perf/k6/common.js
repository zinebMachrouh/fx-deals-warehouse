import http from 'k6/http';
import { check, sleep } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const headers = {
  'Content-Type': 'application/json',
  Accept: 'application/json',
};

function pad2(n) {
  return n < 10 ? `0${n}` : `${n}`;
}

export function formatTimestamp(date = new Date()) {
  const yyyy = date.getFullYear();
  const MM = pad2(date.getMonth() + 1);
  const dd = pad2(date.getDate());
  const HH = pad2(date.getHours());
  const mm = pad2(date.getMinutes());
  const ss = pad2(date.getSeconds());
  // format: yyyy-MM-dd HH:mm:ss
  return `${yyyy}-${MM}-${dd} ${HH}:${mm}:${ss}`;
}

const CURRENCIES = ['USD', 'EUR', 'GBP', 'JPY', 'CHF', 'AUD', 'CAD', 'NZD', 'SEK', 'NOK'];

export function randomCurrencyPair() {
  const fromIdx = Math.floor(Math.random() * CURRENCIES.length);
  let toIdx = Math.floor(Math.random() * CURRENCIES.length);
  if (toIdx === fromIdx) {
    toIdx = (toIdx + 1) % CURRENCIES.length;
  }
  return { from: CURRENCIES[fromIdx], to: CURRENCIES[toIdx] };
}

export function randomAmount(min = 10, max = 100000) {
  const amount = min + Math.random() * (max - min);
  return amount.toFixed(2);
}

export function uniqueDealId(prefix = 'DEAL') {
  // Ensure uniqueness across VUs and iterations
  const rand = Math.floor(Math.random() * 1e9).toString(36);
  return `${prefix}-${__VU}-${__ITER}-${Date.now()}-${rand}`;
}

export function makeDealPayload({ dealId, fromCurrency, toCurrency, amount, timestamp } = {}) {
  const pair = randomCurrencyPair();
  return {
    dealId: dealId || uniqueDealId('FX'),
    fromCurrency: fromCurrency || pair.from,
    toCurrency: toCurrency || pair.to,
    dealTimestamp: timestamp || formatTimestamp(),
    dealAmount: amount || randomAmount(),
  };
}

export function importSingleDeal(payload = makeDealPayload()) {
  const res = http.post(`${BASE_URL}/api/v1/deals/import/single`, JSON.stringify(payload), { headers });
  const ok = check(res, {
    'single import status is 201': (r) => r.status === 201,
  });
  if (!ok) {
    // If failure, log a concise error
    console.error('Import single failed', {
      status: res.status,
      body: res.body && res.body.substring(0, 200),
    });
  }
  return res;
}

export function importBatchDeals(batchSize = 10) {
  const deals = Array.from({ length: batchSize }, () => makeDealPayload());
  const res = http.post(`${BASE_URL}/api/v1/deals/import/batch`, JSON.stringify(deals), { headers });
  // For a valid batch with unique deals, expect 201
  const ok = check(res, {
    'batch import status is 201': (r) => r.status === 201,
  });
  if (!ok) {
    console.error('Import batch failed', {
      status: res.status,
      body: res.body && res.body.substring(0, 200),
    });
  }
  return res;
}

export function getAllDeals() {
  const res = http.get(`${BASE_URL}/api/v1/deals`, { headers });
  check(res, {
    'get all deals status is 200': (r) => r.status === 200,
  });
  return res;
}

export function health(path = '/actuator/health/liveness') {
  const res = http.get(`${BASE_URL}${path}`);
  check(res, {
    'health status 200': (r) => r.status === 200,
  });
  return res;
}

export function think(min = 0.2, max = 1.2) {
  const t = min + Math.random() * (max - min);
  sleep(t);
}

