package com.NBE4_5_SukChanHoSu.BE.global.init;

import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.UserSignUpRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.Gender;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.Genre;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.User;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserProfile;
import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserProfileRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.user.service.UserService;
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

    private final UserProfileRepository userProfileRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Bean
    @Order(1)
    public ApplicationRunner applicationRunner1() {
        return args -> {
            self.profileInit();
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
                    .profileImage("https://example.com/profile" + i + ".jpg")
                    .favoriteGenres(genres) // 장르 리스트 설정
                    .introduce("안녕하세요! 임시 유저 " + i + "입니다.")
                    .latitude(37.5665 + (i * 0.02)) // 임의의 위도 값
                    .longitude(126.9780 + (i * 0.02)) // 임의의 경도 값
                    .searchRadius(20)
                    .user(user) // 유저와 매핑
                    .build();

            // 데이터베이스에 저장
            userProfileRepository.save(userProfile);

            // 로그 출력
            System.out.println("임시 유저 프로필 데이터 생성 완료 (" + i + "):");
            System.out.println("회원가입 완료: " + user.getEmail() + ", 프로필 생성 완료: " + userProfile.getNickName());
            System.out.println("userId: " + userProfile.getUserId());
            System.out.println("nickName: " + userProfile.getNickName());
            System.out.println("gender: " + userProfile.getGender());
            System.out.println("profileImage: " + userProfile.getProfileImage());
            System.out.println("favoriteGenres: " + userProfile.getFavoriteGenres());
            System.out.println("introduce: " + userProfile.getIntroduce());
            System.out.println("latitude: " + userProfile.getLatitude());
            System.out.println("longitude: " + userProfile.getLongitude());
            System.out.println("-----------------------------");
        }
    }

}
