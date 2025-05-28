import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 10, // 가상 사용자 수 (1번부터 10번까지)
    duration: '30s', // 테스트 지속 시간
};

export default function () {
    const baseUrl = 'http://localhost:8080';

    // 1. 로그인 (1번부터 10번까지 바꿔가면서)
    let userId = __VU; // 가상 사용자 번호 (1부터 10까지)
    let loginPayload = JSON.stringify({
        email: `initUser${userId}@example.com`, // initUser1 ~ initUser10
        password: 'testPassword123!', // 테스트용 비밀번호
    });
    let loginResponse = http.post(`${baseUrl}/api/auth/login`, loginPayload, {
        headers: { 'Content-Type': 'application/json' },
    });
    check(loginResponse, {
        'Login Status is 200': (r) => r.status === 200,
    });

    // 2. accessToken 추출
    let accessToken = loginResponse.json('data.accessToken');

    // 3. 박스오피스 조회 API 호출 (파라미터 생략)
    let boxOfficeResponse = http.get(`${baseUrl}/api/movie/weekly`, {
        headers: {
            'Authorization': `Bearer ${accessToken}`,
        },
    });
    check(boxOfficeResponse, {
        'Box Office Status is 200': (r) => r.status === 200,
        'Box Office Response is Valid': (r) => {
            let data = r.json('data');
            return Array.isArray(data) && data.length > 0; // 응답 데이터가 배열이고, 비어 있지 않은지 확인
        },
    });

    sleep(1); // 각 반복 사이에 1초 대기
}