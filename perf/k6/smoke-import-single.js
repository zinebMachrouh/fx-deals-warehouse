import { Trend, Counter } from 'k6/metrics';
import { importSingleDeal, think } from './common.js';

export const options = {
  vus: 1,
  duration: '30s',
};

const tImport = new Trend('smoke_single_import_latency');
const cCreate = new Counter('smoke_single_import_created');

export default function () {
  const res = importSingleDeal();
  tImport.add(res.timings.duration);
  if (res.status === 201) cCreate.add(1);
  think(0.1, 0.5);
}

