import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
    stages: [
        { duration: '1m', target: 3000 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95%의 요청이 500ms 이내에 완료되어야 함
        http_req_failed: ['rate<0.01'],   // 요청 실패율이 1% 미만이어야 함
    },
};

export default function () {
    http.get('https://api.app.mm.ts0608.life/api/monitoring/health');
    sleep(1);
}