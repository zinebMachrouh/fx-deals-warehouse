import { Trend, Counter } from 'k6/metrics';
import { importSingleDeal, importBatchDeals, getAllDeals, think } from './common.js';

// Environment overrides (PowerShell example: $env:SINGLES_STAGE1='10')
const SINGLES_START_RATE = __ENV.SINGLES_START_RATE ? Number(__ENV.SINGLES_START_RATE) : 2;
const SINGLES_STAGE1 = __ENV.SINGLES_STAGE1 ? Number(__ENV.SINGLES_STAGE1) : 5;
const SINGLES_STAGE2 = __ENV.SINGLES_STAGE2 ? Number(__ENV.SINGLES_STAGE2) : 10;
const SINGLES_STAGE1_DURATION = __ENV.SINGLES_STAGE1_DURATION || '30s';
const SINGLES_STAGE2_DURATION = __ENV.SINGLES_STAGE2_DURATION || '1m';
const SINGLES_RAMP_DOWN_DURATION = __ENV.SINGLES_RAMP_DOWN_DURATION || '15s';

const BATCH_RATE = __ENV.BATCH_RATE ? Number(__ENV.BATCH_RATE) : 2;
const BATCH_DURATION = __ENV.BATCH_DURATION || '1m';

const READS_STAGE1 = __ENV.READS_STAGE1 ? Number(__ENV.READS_STAGE1) : 10;
const READS_STAGE2 = __ENV.READS_STAGE2 ? Number(__ENV.READS_STAGE2) : 20;
const READS_STAGE1_DURATION = __ENV.READS_STAGE1_DURATION || '30s';
const READS_STAGE2_DURATION = __ENV.READS_STAGE2_DURATION || '1m';
const READS_RAMP_DOWN_DURATION = __ENV.READS_RAMP_DOWN_DURATION || '30s';

export const options = {
  scenarios: {
    singles: {
      executor: 'ramping-arrival-rate',
      exec: 'singles',
      startRate: SINGLES_START_RATE,
      timeUnit: '1s',
      preAllocatedVUs: __ENV.SINGLES_VUS ? Number(__ENV.SINGLES_VUS) : 10,
      maxVUs: __ENV.SINGLES_MAX_VUS ? Number(__ENV.SINGLES_MAX_VUS) : 50,
      stages: [
        { target: SINGLES_STAGE1, duration: SINGLES_STAGE1_DURATION },
        { target: SINGLES_STAGE2, duration: SINGLES_STAGE2_DURATION },
        { target: 0, duration: SINGLES_RAMP_DOWN_DURATION },
      ],
    },
    batches: {
      executor: 'constant-arrival-rate',
      exec: 'batches',
      rate: BATCH_RATE,
      timeUnit: '1s',
      duration: BATCH_DURATION,
      preAllocatedVUs: __ENV.BATCH_VUS ? Number(__ENV.BATCH_VUS) : 10,
      maxVUs: __ENV.BATCH_MAX_VUS ? Number(__ENV.BATCH_MAX_VUS) : 30,
      startTime: '10s',
    },
    reads: {
      executor: 'ramping-vus',
      exec: 'reads',
      startVUs: 0,
      stages: [
        { target: READS_STAGE1, duration: READS_STAGE1_DURATION },
        { target: READS_STAGE2, duration: READS_STAGE2_DURATION },
        { target: 0, duration: READS_RAMP_DOWN_DURATION },
      ],
      startTime: '5s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'], // overall error rate <2%
    mixed_single_import_latency: ['p(95)<800', 'p(99)<1500'],
    mixed_batch_import_latency: ['p(95)<1200', 'p(99)<2500'],
    mixed_get_all_latency: ['p(95)<500', 'p(99)<1000'],
  },
};

const tSingles = new Trend('mixed_single_import_latency');
const tBatch = new Trend('mixed_batch_import_latency');
const tGet = new Trend('mixed_get_all_latency');
const cSingleCreated = new Counter('mixed_single_created');
const cBatchCreated = new Counter('mixed_batch_created');
const cSingleFailed = new Counter('mixed_single_failed');
const cBatchFailed = new Counter('mixed_batch_failed');
const cReadsFailed = new Counter('mixed_reads_failed');

const BATCH_SIZE = __ENV.BATCH ? Number(__ENV.BATCH) : 10;

export function singles() {
  const res = importSingleDeal();
  tSingles.add(res.timings.duration);
  if (res.status === 201) cSingleCreated.add(1); else cSingleFailed.add(1);
  think(0.1, 0.4);
}

export function batches() {
  const res = importBatchDeals(BATCH_SIZE);
  tBatch.add(res.timings.duration);
  if (res.status === 201) cBatchCreated.add(1); else cBatchFailed.add(1);
  think(0.1, 0.3);
}

export function reads() {
  const res = getAllDeals();
  tGet.add(res.timings.duration);
  if (res.status !== 200) cReadsFailed.add(1);
  think(0.05, 0.2);
}

export function handleSummary(data) {
  const outDir = __ENV.OUTPUT_DIR || 'results';
  return {
    [`${outDir}/mixed-summary.json`]: JSON.stringify(data, null, 2),
  };
}
