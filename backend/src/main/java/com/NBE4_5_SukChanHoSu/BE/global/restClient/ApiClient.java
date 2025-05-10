package com.NBE4_5_SukChanHoSu.BE.global.restClient;

import com.NBE4_5_SukChanHoSu.BE.global.exception.NullResponseException;
import com.NBE4_5_SukChanHoSu.BE.global.exception.movie.ResponseNotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ApiClient {

    private final RestClient restClient;

    public String getResponse(String requestUrl) {
        try {
            String jsonResponse = restClient.get() // get 요청
                    .uri(requestUrl)    // URL 설정
                    .retrieve()     // 응답
                    .body(String.class);    // String 변환

            // 응답이 비어있는 경우
            if (jsonResponse == null || jsonResponse.isEmpty()) {
                throw new ResponseNotFound("404","KOBIS 응답 요청이 비어있습니다.");
            }
            return jsonResponse;
        } catch (Exception e) {
            throw new NullResponseException("404","응답이 비어있습니다.");
        }
    }

}
