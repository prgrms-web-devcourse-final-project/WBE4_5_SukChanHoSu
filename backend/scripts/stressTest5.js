import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
    stages: [
        { duration: '1m', target: 100 },  // 1분 동안 100명의 사용자로 증가
        { duration: '2m', target: 500 },  // 2분 동안 500명의 사용자로 증가
        { duration: '3m', target: 1000 }, // 3분 동안 1000명의 사용자로 증가
        { duration: '2m', target: 1500 }, // 2분 동안 1500명의 사용자로 증가
        { duration: '1m', target: 2000 }, // 1분 동안 2000명의 사용자로 증가
        { duration: '1m', target: 2500 }, // 1분 동안 2500명의 사용자로 증가
        { duration: '1m', target: 0 },    // 1분 동안 사용자 수를 0으로 감소 (정리 단계)
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