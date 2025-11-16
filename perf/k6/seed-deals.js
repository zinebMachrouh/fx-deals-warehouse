import { importSingleDeal, think } from './common.js';
import { Counter } from 'k6/metrics';

export const options = {
  vus: 1,
  iterations: __ENV.SEED_COUNT ? Number(__ENV.SEED_COUNT) : 50,
};

const cSeedCreated = new Counter('seed_created');
const cSeedFailed = new Counter('seed_failed');

export default function () {
  const res = importSingleDeal();
  if (res.status === 201) {
    cSeedCreated.add(1);
  } else {
    cSeedFailed.add(1);
  }
  think(0.01, 0.05);
}

