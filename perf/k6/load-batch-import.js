import { Trend, Counter } from 'k6/metrics';
import { importBatchDeals, think } from './common.js';

export const options = {
  scenarios: {
    steady_batch: {
      executor: 'constant-arrival-rate',
      rate: __ENV.RATE ? Number(__ENV.RATE) : 10, // requests per second
      timeUnit: '1s',
      duration: __ENV.DURATION || '2m',
      preAllocatedVUs: __ENV.VUS ? Number(__ENV.VUS) : 10,
      maxVUs: __ENV.MAX_VUS ? Number(__ENV.MAX_VUS) : 50,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
  },
};

const tBatch = new Trend('load_batch_import_latency');
const cBatchCreated = new Counter('load_batch_import_created');

const BATCH_SIZE = __ENV.BATCH ? Number(__ENV.BATCH) : 10;

export default function () {
  const res = importBatchDeals(BATCH_SIZE);
  tBatch.add(res.timings.duration);
  if (res.status === 201) cBatchCreated.add(1);
  think(0.1, 0.3);
}

