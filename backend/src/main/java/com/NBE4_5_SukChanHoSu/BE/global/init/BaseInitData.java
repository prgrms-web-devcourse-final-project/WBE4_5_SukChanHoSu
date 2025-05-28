package com.NBE4_5_SukChanHoSu.BE.global.init;

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

    private static final String LIKE_STREAM = "like";
    private static final String MATCH_STREAM = "matching";

    @Bean
    @Order(1)
    public ApplicationRunner applicationRunner1() {
        return args -> {
            self.profileInit();
            self.movieInit();
            self.reviewInit();
        };
    }


    @Transactional
    public void profileInit() {
        if (userRepository.count() > 0) {
            System.out.println("⚠️ 유저가 이미 존재하여 profileInit() 스킵됨.");
            return;
        }

        Random random = new Random();

        for (int i = 1; i <= 10; i++) {
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
}
