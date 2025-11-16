import { sleep } from 'k6';
import { health } from './common.js';

export const options = {
  vus: 1,
  duration: '30s',
};

export default function () {
  health('/actuator/health/liveness');
  sleep(1);
}

