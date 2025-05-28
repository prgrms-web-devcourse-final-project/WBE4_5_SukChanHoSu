package com.NBE4_5_SukChanHoSu.BE.global.init;

import com.NBE4_5_SukChanHoSu.BE.domain.likes.dto.response.MatchingResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.likes.service.UserLikeService;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.entity.Movie;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.repository.MovieRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.review.dto.request.ReviewCreateDto;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.review.entity.Review;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.review.repository.ReviewRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.movie.review.service.ReviewService;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.UserSignUpRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.*;
import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserProfileRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {

    @Autowired
    @Lazy
    private BaseInitData self;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private MovieRepository movieRepository;

    private final UserLikeService userLikeService;

    private static final String LIKE_STREAM = "like";
    private static final String MATCH_STREAM = "matching";

    @Bean
    @Order(1)
    public ApplicationRunner applicationRunner1() {
        return args -> {
            self.profileInit();
            self.movieInit();
            self.reviewInit();
            self.likeInit1();
            self.likeInit2();
            self.likeInit3();
            self.likeInit4();
            self.likeInit5();
        };
    }


    @Transactional
    public void profileInit() {
        if (userRepository.count() > 0) {
            System.out.println("⚠️ 유저가 이미 존재하여 profileInit() 스킵됨.");
            return;
        }

        Random random = new Random();

        for (int i = 1; i <= 30; i++) {
            String newEmail = "initUser" + i + "@example.com";
            redisTemplate.opsForValue().set("emailVerify:" + newEmail, "true", 5, TimeUnit.MINUTES);
            UserSignUpRequest signUpDto = UserSignUpRequest.builder()
                    .email(newEmail)
                    .password("testPassword123!")
                    .passwordConfirm("testPassword123!")
                    .build();

            User user = userService.join(signUpDto);
            userRepository.save(user); // 저장
            userRepository.flush(); // 갱신

            // 랜덤 장르 3개 선택
            List<Genre> genres = Stream.of(Genre.values())
                    .sorted((genre1, genre2) -> random.nextInt(2) - 1)
                    .limit(3) // 상위 3개 선택
                    .collect(Collectors.toList());

            // 빌더 패턴으로 프로필 생성
            UserProfile userProfile = UserProfile.builder()
                    .nickName("TempUser" + i)
                    .gender(i % 2 == 0 ? Gender.Female : Gender.Male)
                    .profileImages(List.of("https://example.com/profile" + i + ".jpg"))
                    .favoriteGenres(genres) // 장르 리스트 설정
                    .introduce("안녕하세요! 임시 유저 " + i + "입니다.")
                    .latitude(37.5665 + (i * 0.01)) // 임의의 위도 값
                    .longitude(126.9780 + (i * 0.01)) // 임의의 경도 값
                    .searchRadius(20)
                    .user(user) // 유저와 매핑
                    .build();

            user.setRole(Role.ADMIN);
            // 데이터베이스에 저장
            userProfileRepository.save(userProfile);

            // 로그 출력
            System.out.println("임시 유저 프로필 데이터 생성 완료 (" + i + "):");
            System.out.println("회원가입 완료: " + user.getEmail() + ", 프로필 생성 완료: " + userProfile.getNickName());
            System.out.println("userId: " + userProfile.getUserId());
            System.out.println("nickName: " + userProfile.getNickName());
            System.out.println("gender: " + userProfile.getGender());
            System.out.println("profileImage: " + userProfile.getProfileImages());
            System.out.println("favoriteGenres: " + userProfile.getFavoriteGenres());
            System.out.println("introduce: " + userProfile.getIntroduce());
            System.out.println("latitude: " + userProfile.getLatitude());
            System.out.println("longitude: " + userProfile.getLongitude());
            System.out.println("-----------------------------");
        }
    }

    @Transactional
    public void reviewInit() {
        if (reviewRepository.count() > 0) {
            System.out.println("⚠️ 리뷰가 이미 존재하여 reviewInit() 스킵됨.");
            return;
        }

        List<User> users = userRepository.findAll();
        List<Movie> movies = movieRepository.findAll();
        Random random = new Random();

        for (User user : users) {
            Movie movie = movies.getFirst();
            double rating = 2.5 + random.nextDouble() * 2.5; // 2.5 ~ 5.0

            ReviewCreateDto reviewDto = new ReviewCreateDto();
            reviewDto.setMovieId(movie.getMovieId()); // ← movieId 설정
            reviewDto.setContent("이 영화 정말 재미있었어요! (" + movie.getTitle() + "에 대한 리뷰입니다)");
            reviewDto.setRating(Math.round(rating * 10.0) / 10.0); // 소수점 1자리 반올림

            reviewService.initCreateReviewPost(reviewDto, user);
        }

        List<Review> reviews = reviewRepository.findAllByMovie_MovieId(20070001L);
        reviews.getFirst().setLikeCount(10);
        reviews.get(1).setLikeCount(8);
        reviews.get(2).setLikeCount(6);
    }


    @Transactional
    public void movieInit() {
        if (reviewRepository.count() > 0) {
            return;
        }

        List<Movie> movies = List.of(
                Movie.builder()
                        .movieId(20070001L)
                        .title("Inception")
                        .genresRaw("Action, Science Fiction")
                        .releaseDate("20100716")
                        .posterImage("https://image.tmdb.org/t/p/w500/qmDpIHrmpJINaRKAfWQfftjCdyi.jpg")
                        .description("꿈속의 꿈으로 들어가는 액션 블록버스터")
                        .director("Christopher Nolan")
                        .build(),

                Movie.builder()
                        .movieId(20070002L)
                        .title("The Matrix")
                        .genresRaw("Action, Science Fiction")
                        .releaseDate("19990331")
                        .posterImage("https://image.tmdb.org/t/p/w500/aZiK1mzNHRn7kvVxU3lK1ElGNRk.jpg")
                        .description("가상현실과 인간의 전쟁")
                        .director("Lana Wachowski, Lilly Wachowski")
                        .build(),

                Movie.builder()
                        .movieId(20070003L)
                        .title("example")
                        .genresRaw("Action, Comedy")
                        .releaseDate("19990331")
                        .posterImage("https://image.tmdb.org/t/p/w500/aZiK1mzNHRn7kvVxU3lK1ElGNRk.jpg")
                        .description("예시의 영화")
                        .director("Lana Wachowski, Lilly Wachowski")
                        .build()
        );
        movieRepository.saveAll(movies);
    }

    @PostConstruct
    public void init() {
        if (!redisTemplate.hasKey(LIKE_STREAM)) {
            redisTemplate.opsForStream().createGroup(LIKE_STREAM, "like-group");
        }
        if (!redisTemplate.hasKey(MATCH_STREAM)) {
            redisTemplate.opsForStream().createGroup(MATCH_STREAM, "match-group");
        }
    }

    @Transactional
    public void likeInit1() {
        // 홀수 유저 ID 리스트
        List<Long> oddUserIds = List.of(1L, 3L, 5L, 7L, 9L);

        // 짝수 유저 ID 리스트 (2, 4, 6, 8, 10)
        List<Long> evenUserIds = List.of(2L, 4L, 6L, 8L, 10L);

        // 홀수 유저가 짝수 유저에게 like 전송
        for (Long oddUserId : oddUserIds) {
            for (Long evenUserId : evenUserIds) {
                // 유저 프로필 조회
                UserProfile fromUser = userProfileRepository.findById(oddUserId)
                        .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다: " + oddUserId));
                UserProfile toUser = userProfileRepository.findById(evenUserId)
                        .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다: " + evenUserId));

                // 이미 like를 보낸 상태인지 확인
                if (!userLikeService.isAlreadyLikes(fromUser, toUser) && !userLikeService.isAlreadyMatched(fromUser, toUser)) {
                    // like 전송
                    userLikeService.likeUser(fromUser, toUser);
                    System.out.println("유저 " + oddUserId + "가 유저 " + evenUserId + "에게 like를 전송했습니다.");

                    // 매칭 상태 확인 및 처리
                    if (userLikeService.isAlreadyLiked(fromUser, toUser)) {
                        MatchingResponse response = userLikeService.matching(fromUser, toUser);
                        System.out.println("매칭 완료: 유저 " + oddUserId + "와 유저 " + evenUserId);
                    }
                } else {
                    System.out.println("유저 " + oddUserId + "는 이미 유저 " + evenUserId + "에게 like를 보낸 상태입니다.");
                }
            }
        }

        // 짝수 유저가 홀수 유저에게 like 전송
        for (Long evenUserId : evenUserIds) {
            for (Long oddUserId : oddUserIds) {
                // 유저 프로필 조회
                UserProfile fromUser = userProfileRepository.findById(evenUserId)
                        .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다: " + evenUserId));
                UserProfile toUser = userProfileRepository.findById(oddUserId)
                        .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다: " + oddUserId));

                // 이미 like를 보낸 상태인지 확인
                if (!userLikeService.isAlreadyLikes(fromUser, toUser) && !userLikeService.isAlreadyMatched(fromUser, toUser)) {
                    // like 전송
                    userLikeService.likeUser(fromUser, toUser);
                    System.out.println("유저 " + evenUserId + "가 유저 " + oddUserId + "에게 like를 전송했습니다.");

                    // 매칭 상태 확인 및 처리
                    if (userLikeService.isAlreadyLiked(fromUser, toUser)) {
                        MatchingResponse response = userLikeService.matching(fromUser, toUser);
                        System.out.println("매칭 완료: 유저 " + evenUserId + "와 유저 " + oddUserId);
                    }
                } else {
                    System.out.println("유저 " + evenUserId + "는 이미 유저 " + oddUserId + "에게 like를 보낸 상태입니다.");
                }
            }
        }

        System.out.println("likeInit1 더미 데이터 생성 완료!");
    }

    @Transactional
    public void likeInit2() {
        // 짝수 유저 ID 리스트 (12, 14, 16, 18, 20)
        List<Long> evenUserIds = Stream.iterate(12L, n -> n + 2)
                .limit(5) // 12부터 20까지 5개의 짝수
                .collect(Collectors.toList());

        // 홀수 유저 ID 리스트 (1, 3, 5, 7, 9)
        List<Long> oddUserIds = List.of(1L, 3L, 5L, 7L, 9L);

        // 짝수 유저가 홀수 유저에게 like 전송
        for (Long evenUserId : evenUserIds) {
            for (Long oddUserId : oddUserIds) {
                // 유저 프로필 조회
                UserProfile fromUser = userProfileRepository.findById(evenUserId)
                        .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다: " + evenUserId));
                UserProfile toUser = userProfileRepository.findById(oddUserId)
                        .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다: " + oddUserId));

                // 이미 like를 보낸 상태인지 확인
                if (!userLikeService.isAlreadyLikes(fromUser, toUser) && !userLikeService.isAlreadyMatched(fromUser, toUser)) {
                    // like 전송
                    userLikeService.likeUser(fromUser, toUser);
                    System.out.println("유저 " + evenUserId + "가 유저 " + oddUserId + "에게 like를 전송했습니다.");

                    // 매칭 상태 확인 및 처리
                    if (userLikeService.isAlreadyLiked(fromUser, toUser)) {
                        MatchingResponse response = userLikeService.matching(fromUser, toUser);
                        System.out.println("매칭 완료: 유저 " + evenUserId + "와 유저 " + oddUserId);
                    }
                } else {
                    System.out.println("유저 " + evenUserId + "는 이미 유저 " + oddUserId + "에게 like를 보낸 상태입니다.");
                }
            }
        }

        System.out.println("likeInit2 더미 데이터 생성 완료!");
    }

    @Transactional
    public void likeInit3() {
        // 홀수 유저 ID 리스트 (1, 3, 5, 7, 9)
        List<Long> oddUserIds = List.of(1L, 3L, 5L, 7L, 9L);

        // 짝수 유저 ID 리스트 (22, 24, 26, 28, 30)
        List<Long> evenUserIds = Stream.iterate(22L, n -> n + 2)
                .limit(5) // 22부터 30까지 5개의 짝수
                .collect(Collectors.toList());

        // 홀수 유저가 짝수 유저에게 like 전송
        for (Long oddUserId : oddUserIds) {
            for (Long evenUserId : evenUserIds) {
                // 유저 프로필 조회
                UserProfile fromUser = userProfileRepository.findById(oddUserId)
                        .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다: " + oddUserId));
                UserProfile toUser = userProfileRepository.findById(evenUserId)
                        .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다: " + evenUserId));

                // 이미 like를 보낸 상태인지 확인
                if (!userLikeService.isAlreadyLikes(fromUser, toUser) && !userLikeService.isAlreadyMatched(fromUser, toUser)) {
                    // like 전송
                    userLikeService.likeUser(fromUser, toUser);
                    System.out.println("유저 " + oddUserId + "가 유저 " + evenUserId + "에게 like를 전송했습니다.");

                    // 매칭 상태 확인 및 처리
                    if (userLikeService.isAlreadyLiked(fromUser, toUser)) {
                        MatchingResponse response = userLikeService.matching(fromUser, toUser);
                        System.out.println("매칭 완료: 유저 " + oddUserId + "와 유저 " + evenUserId);
                    }
                } else {
                    System.out.println("유저 " + oddUserId + "는 이미 유저 " + evenUserId + "에게 like를 보낸 상태입니다.");
                }
            }
        }

        System.out.println("likeInit3 더미 데이터 생성 완료!");
    }

    @Transactional
    public void likeInit4() {
        // 홀수 유저 ID 리스트 (11, 13, 15, 17, 19)
        List<Long> oddUserIds = Stream.iterate(11L, n -> n + 2)
                .limit(5) // 11부터 19까지 5개의 홀수
                .collect(Collectors.toList());

        // 짝수 유저 ID 리스트 (2, 4, 6, 8, 10)
        List<Long> evenUserIds = List.of(2L, 4L, 6L, 8L, 10L);

        // 홀수 유저가 짝수 유저에게 like 전송
        for (Long oddUserId : oddUserIds) {
            for (Long evenUserId : evenUserIds) {
                // 유저 프로필 조회
                UserProfile fromUser = userProfileRepository.findById(oddUserId)
                        .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다: " + oddUserId));
                UserProfile toUser = userProfileRepository.findById(evenUserId)
                        .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다: " + evenUserId));

                // 이미 like를 보낸 상태인지 확인
                if (!userLikeService.isAlreadyLikes(fromUser, toUser) && !userLikeService.isAlreadyMatched(fromUser, toUser)) {
                    // like 전송
                    userLikeService.likeUser(fromUser, toUser);
                    System.out.println("유저 " + oddUserId + "가 유저 " + evenUserId + "에게 like를 전송했습니다.");

                    // 매칭 상태 확인 및 처리
                    if (userLikeService.isAlreadyLiked(fromUser, toUser)) {
                        MatchingResponse response = userLikeService.matching(fromUser, toUser);
                        System.out.println("매칭 완료: 유저 " + oddUserId + "와 유저 " + evenUserId);
                    }
                } else {
                    System.out.println("유저 " + oddUserId + "는 이미 유저 " + evenUserId + "에게 like를 보낸 상태입니다.");
                }
            }
        }

        System.out.println("likeInit4 더미 데이터 생성 완료!");
    }

    @Transactional
    public void likeInit5() {
        // 홀수 유저 ID 리스트 (1, 3, 5, 7, 9)
        List<Long> oddUserIds = List.of(2L, 4L, 6L, 8L, 10L);

        // 짝수 유저 ID 리스트 (22, 24, 26, 28, 30)
        List<Long> evenUserIds = Stream.iterate(21L, n -> n + 2)
                .limit(5) // 22부터 30까지 5개의 짝수
                .collect(Collectors.toList());

        // 홀수 유저가 짝수 유저에게 like 전송
        for (Long oddUserId : oddUserIds) {
            for (Long evenUserId : evenUserIds) {
                // 유저 프로필 조회
                UserProfile fromUser = userProfileRepository.findById(oddUserId)
                        .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다: " + oddUserId));
                UserProfile toUser = userProfileRepository.findById(evenUserId)
                        .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다: " + evenUserId));

                // 이미 like를 보낸 상태인지 확인
                if (!userLikeService.isAlreadyLikes(fromUser, toUser) && !userLikeService.isAlreadyMatched(fromUser, toUser)) {
                    // like 전송
                    userLikeService.likeUser(fromUser, toUser);
                    System.out.println("유저 " + oddUserId + "가 유저 " + evenUserId + "에게 like를 전송했습니다.");

                    // 매칭 상태 확인 및 처리
                    if (userLikeService.isAlreadyLiked(fromUser, toUser)) {
                        MatchingResponse response = userLikeService.matching(fromUser, toUser);
                        System.out.println("매칭 완료: 유저 " + oddUserId + "와 유저 " + evenUserId);
                    }
                } else {
                    System.out.println("유저 " + oddUserId + "는 이미 유저 " + evenUserId + "에게 like를 보낸 상태입니다.");
                }
            }
        }

        System.out.println("likeInit3 더미 데이터 생성 완료!");
    }

}
