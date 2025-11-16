import { Trend } from 'k6/metrics';
import { getAllDeals, think } from './common.js';

export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '1m', target: 25 },
    { duration: '1m', target: 50 },
    { duration: '1m', target: 100 },
    { duration: '30s', target: 0 },
  ],
};

const tGet = new Trend('stress_get_all_latency');

export default function () {
  const res = getAllDeals();
  tGet.add(res.timings.duration);
  think(0.05, 0.2);
}

