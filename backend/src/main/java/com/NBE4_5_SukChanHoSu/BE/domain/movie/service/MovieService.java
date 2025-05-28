package com.NBE4_5_SukChanHoSu.BE.domain.movie.service;

import com.NBE4_5_SukChanHoSu.BE.domain.movie.dto.response.MovieRankingResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.dto.response.MovieResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.dto.WeeklyBoxOfficeResult;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.entity.MovieGenre;
import com.NBE4_5_SukChanHoSu.BE.global.exception.NullResponseException;
import com.NBE4_5_SukChanHoSu.BE.global.exception.movie.ParsingException;
import com.NBE4_5_SukChanHoSu.BE.global.exception.redis.RedisSerializationException;
import com.NBE4_5_SukChanHoSu.BE.global.redis.config.RedisTTL;
import com.NBE4_5_SukChanHoSu.BE.global.restClient.ApiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieService {

    @Value("${movie.api.key}")
    private String kobisApiKey; // KOBIS API 키

    @Value("${movie.api.key2}")
    private String tmdbApiKey; // TMDB API 키

    @Value("${movie.api.rank-url}")
    private String rankUrl; // KOBIS 주간 박스오피스 API URL

    @Value("${movie.api.detail-url}")
    private String detailUrl; // KOBIS 영화 상세 정보 API URL

    @Value("${movie.api.tmdb-search-url}")
    private String tmdbSearchUrl; // TMDB 영화 검색 API URL

    private final RedisTemplate<String,Object> redisTemplate;
    private final RedisTTL ttl;
    private final RestClient restClient;  // // redisTemplate -> webClient -> RestClient 로 외부 수집 프로토콜 변경
    private final ObjectMapper objectMapper;
    private final ApiClient apiClient;

    private static final String BOX_OFFICE_KEY = "weeklyBoxOffice";
    private static final String MOVIE_KEY = "MovieCd:";
    // 주간 박스오피스
    public List<MovieRankingResponse> searchWeeklyBoxOffice(String targetDt, String weekGb, String itemPerPage) {
        // 캐시 먼저 확인
        if(redisTemplate.hasKey(BOX_OFFICE_KEY)){
            // 캐싱된 데이터 반환
            return (List<MovieRankingResponse>) redisTemplate.opsForValue().get(BOX_OFFICE_KEY);
        }

        // 요청 URL 생성
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(rankUrl)
                .queryParam("key", kobisApiKey)
                .queryParam("targetDt", targetDt)
                .queryParam("weekGb", weekGb)
                .queryParam("itemPerPage", itemPerPage);

        // 요청 URL
        String requestUrl = builder.toUriString();

        // 응답 JSON으로 받아오기
        String jsonResponse = apiClient.getResponse(requestUrl);

        // JSON 파싱/데이터 처리
        return extractBoxOffice(jsonResponse);
    }

    // 영화 상세 페이지
    public MovieResponse getMovieDetail(String movieCd) {
        String key = MOVIE_KEY + movieCd;
        // 캐싱 데이터 먼저 확인
        MovieResponse cachedData = getCahedData(key);
        if(cachedData != null){
            return cachedData;
        }

        // 요청 URL 생성
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(detailUrl)
                .queryParam("key", kobisApiKey)
                .queryParam("movieCd", movieCd);

        String requestUrl = builder.toUriString();

        // API 요청 및 응답 받기
        String jsonResponse = apiClient.getResponse(requestUrl);

        // JSON 파싱 및 정보 추출
        MovieResponse response = extractMovieDetail(jsonResponse);

        // 캐싱
        saveToRedis(response,key);

        return response;
    }

    // TMDB에서 영화 상세 정보 가져오기
    private Map<String, Object> getTmdbMovieInfo(String movieNm) {
        // 요청 URL 생성
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(tmdbSearchUrl)
                .queryParam("api_key", tmdbApiKey)
                .queryParam("query", movieNm);

        String requestUrl = builder.toUriString();

        // API 요청 및 응답 받기
        String jsonResponse = apiClient.getResponse(requestUrl);

        // JSON 파싱 및 상세 정보 추출
        return extractTmdbMovieInfo(jsonResponse);
    }

    // TMDB 영화 정보 파싱
    public Map<String,Object> extractTmdbMovieInfo(String jsonResponse) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> results = (List<Map<String, Object>>) responseMap.get("results");
            if (results == null || results.isEmpty()) {
                return null;
            }

            // 첫 번째 결과의 ID를 사용하여 상세 정보 조회
            Map<String, Object> first = results.getFirst();
            String movieId = first.get("id").toString();

            // 디테일 URL 생성
            String detailUrl = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + tmdbApiKey;
            // 응답 생성
            String detailResponse = restClient.get() // get 요청
                    .uri(detailUrl)    // URL 설정
                    .retrieve()     // 응답
                    .body(String.class);    // String 변환

            // 상세 정보 반환
            return objectMapper.readValue(detailResponse, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new ParsingException("400","API 응답 파싱 실패");
        }
    }

    // 포스터 가져오기
    private String getMoviePoster(String movieNm) {
        // 요청 URL 생성
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(tmdbSearchUrl)
                .queryParam("api_key", tmdbApiKey)
                .queryParam("query", movieNm);

        String requestUrl = builder.toUriString();

        // API 요청 및 응답
        String jsonResponse = apiClient.getResponse(requestUrl);

        return getPostUrl(jsonResponse);
    }

    // JSON 파싱 및 포스터 URL 추출
    private String getPostUrl(String jsonResponse) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> results = (List<Map<String, Object>>) responseMap.get("results");
            if (results == null || results.isEmpty()) {
                return "포스터 정보 없음";
            }

            Map<String, Object> first = results.getFirst();

            // 포스터 URL 추출
            String posterPath = (String) first.get("poster_path");
            // 포스터 URL 생성
            return "https://image.tmdb.org/t/p/w500" + posterPath;
        } catch (Exception e) {
            throw new ParsingException("400","API 응답 파싱 실패");
        }

    }

    // 보고싶은 영화 등록
    @Transactional
    public String bookmarkMovie(long profileId, String movieCd) {
        String key = "user:" + profileId;   // Redis에 저장된 key(like 전송시 생성)
        redisTemplate.opsForValue().set(key,movieCd,ttl.getData(),TimeUnit.SECONDS);    // 1주일 저장
        return getBookmarkDataFromRedis(key);
    }

    // 레디스에 저장된 데이터
    public String getBookmarkDataFromRedis(String key) {
        if(redisTemplate.hasKey(key)){
            return Optional.ofNullable((String)redisTemplate.opsForValue().get(key))
                    .orElseThrow(() -> new NullResponseException("404","데이터가 비어있습니다."));
        }
        throw new RedisSerializationException("500","레디스 키 저장 실패");
    }

    // 레디스에 저장
    private void saveToRedis(MovieResponse response, String key) {
        try{
            // MovieResponse -> Json 문자열로 직렬화하여 저장
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json,ttl.getDetail(),TimeUnit.SECONDS);    // 1주일
        } catch (ParsingException e) {
            throw new ParsingException("400","캐시 데이터 직렬화 실패");
        } catch (Exception e) {
            throw new ParsingException("400","API 응답 파싱 실패");
        }
    }

    // 캐싱 데이터 가져오기
    public MovieResponse getCahedData(String key) {
        if(redisTemplate.hasKey(key)){
            String cachedData =(String) redisTemplate.opsForValue().get(key);
            if(cachedData != null){
                try{
                    // 직렬화
                    return objectMapper.readValue(cachedData, MovieResponse.class);
                }catch (Exception e){
                    throw new ParsingException("400","캐시 데이터 역직렬화 실패");
                }
            }
        }
        return null;
    }

    // 박스오피스 데이터 파싱
    private List<MovieRankingResponse> extractBoxOffice(String jsonResponse) {
        try {
            // Json -> WeeklyBoxOfficeResult 로 파싱
            WeeklyBoxOfficeResult result = objectMapper.readValue(jsonResponse, WeeklyBoxOfficeResult.class);
            if (result == null) {
                throw new ParsingException("400","API 응답 파싱 실패");
            }

            // 박스오피스 리스트
            List<WeeklyBoxOfficeResult.WeeklyBoxOffice> boxOfficeList = result.getBoxOfficeResult().getWeeklyBoxOfficeList();
            // 처리 데이터가 널
            if (boxOfficeList == null || boxOfficeList.isEmpty()) {
                throw new NullResponseException("404","응답이 비어있습니다.");
            }

            // 영화 상세 정보 조회
            List<MovieRankingResponse> responses = boxOfficeList.stream()
                    .map(movie -> {
                        String posterUrl = getMoviePoster(movie.getMovieNm()); // TMDB에서 포스터 URL 조회
                        return new MovieRankingResponse(
                                movie.getRank(), // 순위
                                movie.getMovieNm(), // 영화명
                                posterUrl, // 포스터 URL
                                movie.getAudiAcc(),
                                movie.getMovieCd()
                        );
                    })
                    .collect(Collectors.toList());
            // 캐싱
            redisTemplate.opsForValue().set(BOX_OFFICE_KEY, responses, ttl.getRank(), TimeUnit.SECONDS); // TTL 설정
            return responses;
        } catch (Exception e) {
            throw new ParsingException("400","API 응답 파싱 실패");
        }
    }

    // 영화 상세정보 파싱
    private MovieResponse extractMovieDetail(String jsonResponse) {
        try{
            Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});

            // 결과 저장
            Map<String, Object> movieInfoResult = (Map<String, Object>) responseMap.get("movieInfoResult");
            if (movieInfoResult == null) {
                throw new NullResponseException("404","응답이 비어있습니다.");
            }

            // 영화 정보 저장
            Map<String, Object> movieInfo = (Map<String, Object>) movieInfoResult.get("movieInfo");
            if (movieInfo == null) {
                throw new NullResponseException("404","응답이 비어있습니다.");
            }

            // 영화명 추출 (널 체크 및 기본값 설정)
            String movieNm = movieInfo.get("movieNm") != null ? movieInfo.get("movieNm").toString() : "영화명 정보 없음";

            // 개봉일 추출 (널 체크 및 기본값 설정)
            String openDt = movieInfo.get("openDt") != null ? movieInfo.get("openDt").toString() : "개봉일 정보 없음";

            // 상영 시간 추출 (널 체크 및 기본값 설정)
            String showTm = movieInfo.get("showTm") != null ? movieInfo.get("showTm").toString() : "상영 시간 정보 없음";

            // 감독 정보 추출 (단일 String으로 처리)
            List<Map<String, String>> directors = (List<Map<String, String>>) movieInfo.get("directors");
            String director = "감독 정보 없음"; // 기본값
            if (directors != null && !directors.isEmpty()) {
                Map<String, String> first = directors.getFirst();
                if (first != null && first.get("peopleNm") != null) {
                    director = first.get("peopleNm");
                }
            }

            // 배우 정보 추출
            List<Map<String, String>> actors = (List<Map<String, String>>) movieInfo.get("actors");
            String actorList = (actors == null || actors.isEmpty())
                    ? "배우 정보 없음"
                    : actors.stream()
                    .map(actor -> actor.get("peopleNm"))
                    .collect(Collectors.joining(", ")); // 쉼표로 연결

            // 연령제한 정보 추출
            List<Map<String, String>> audits = (List<Map<String, String>>) movieInfo.get("audits");
            String watchGradeNm ="연령제한 정보 없음"; // 기본값
            if(audits != null && !audits.isEmpty()){
                Map<String,String> first = audits.getFirst();
                watchGradeNm = first.get("watchGradeNm");
            }

            // 포스터 URL 및 TMDB 장르, 줄거리 조회
            String posterUrl = getMoviePoster(movieNm);
            String overview = "줄거리 정보 없음"; // 기본값
            List<MovieGenre> genreList = List.of(new MovieGenre("장르 정보 없음")); // 기본값

            Map<String, Object> tmdbMovieInfo = getTmdbMovieInfo(movieNm);
            if (tmdbMovieInfo != null) {
                // 줄거리
                overview = Optional.ofNullable(tmdbMovieInfo.get("overview"))
                        .map(Object::toString)
                        .orElse("줄거리 정보 없음");

                // 장르 추출
                List<Map<String, Object>> genres = (List<Map<String, Object>>) tmdbMovieInfo.get("genres");
                if (genres != null && !genres.isEmpty()) {
                    // 장르 객체로 생성
                    genreList = genres.stream()
                            .map(genre -> new MovieGenre(genre.get("name").toString()))
                            .collect(Collectors.toList());
                }
            }

            // MovieResponse 객체 생성 및 반환
            return new MovieResponse(
                    movieNm, // 영화명
                    openDt, // 개봉일
                    showTm, // 상영 시간
                    director, // 감독 (String)
                    genreList, // 장르 (TMDB)
                    actorList, // 배우
                    watchGradeNm, // 연령제한
                    posterUrl, // 포스터 URL
                    overview // 줄거리 (TMDB)
            );
        } catch (Exception e) {
            throw new ParsingException("400","API 응답 파싱 실패");
        }
    }
}