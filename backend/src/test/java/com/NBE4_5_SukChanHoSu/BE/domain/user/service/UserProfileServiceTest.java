package com.NBE4_5_SukChanHoSu.BE.domain.user.service;

import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.User;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.ProfileRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.ProfileResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request.ProfileUpdateRequest;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserProfile;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.Gender;
import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserProfileRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserRepository;
import com.NBE4_5_SukChanHoSu.BE.global.util.S3Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Util s3Util;

    @InjectMocks
    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("createProfile 테스트")
    class CreateProfileTest {
        void createProfile_success() throws IOException {
            Long userId = 1L;

            User user = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .name("Test User")
                    .build();

            UserProfile savedUserProfile = UserProfile.builder()
                    .userId(userId)
                    .nickName("testuser")
                    .gender(Gender.Male)
                    .profileImage("s3://profile.jpg")
                    .latitude(37.5665)
                    .longitude(126.9780)
                    .birthdate(LocalDate.of(2000, 1, 1))
                    .introduce("소개입니다.")
                    .searchRadius(10)
                    .lifeMovie("인생영화")
                    .favoriteGenres(null)
                    .watchedMovies(null)
                    .preferredTheaters(null)
                    .user(user)
                    .build();

            ProfileRequest dto = ProfileRequest.builder()
                    .nickname("testuser")
                    .gender(Gender.Male)
                    .latitude(37.5665)
                    .longitude(126.9780)
                    .birthdate(LocalDate.of(2000, 1, 1))
                    .introduce("소개입니다.")
                    .searchRadius(10)
                    .lifeMovie("인생영화")
                    .favoriteGenres(null)
                    .watchedMovies(null)
                    .preferredTheaters(null)
                    .build();

            MultipartFile mockFile = mock(MultipartFile.class);
            when(mockFile.isEmpty()).thenReturn(false);
            when(s3Util.uploadFile(mockFile)).thenReturn("s3://profile.jpg");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(userProfileRepository.save(any(UserProfile.class))).thenReturn(savedUserProfile);

            ProfileResponse response = userProfileService.createProfile(userId, dto, null); // 이미지 URL 직접 전달

            assertThat(response.getNickname()).isEqualTo(dto.getNickname());
            assertThat(response.getProfileImage()).isEqualTo("s3://profile.jpg");
            verify(userProfileRepository, times(1)).save(any(UserProfile.class));
            verify(s3Util, never()).uploadFile(any(MultipartFile.class)); // createProfile 시에는 컨트롤러에서 업로드하므로 여기서는 호출 안됨
        }
        @Test
        @DisplayName("이미 프로필이 등록된 경우 예외를 던진다.")
        void createProfile_alreadyExists() throws IOException {
            // given
            Long userId = 1L;

            // User 객체 생성 및 Mocking
            User user = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .name("Test User")
                    .build();
            UserProfile existingProfile = UserProfile.builder()
                    .userId(userId)
                    .nickName("alreadySet")
                    .user(user)
                    .build();
            ProfileRequest dto = ProfileRequest.builder()
                    .nickname("testuser")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));

            // when & then
            assertThatThrownBy(() -> userProfileService.createProfile(userId, dto, null)) // 이미지 URL null로 전달
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("이미 프로필이 등록된 사용자입니다.");

            verify(userProfileRepository, never()).save(any(UserProfile.class));
            verify(s3Util, never()).uploadFile(any(MultipartFile.class));
        }
    }

        @Nested
        @DisplayName("updateProfile 테스트")
        class UpdateProfileTest {

            @Test
            @DisplayName("정상적으로 프로필을 수정하고 S3 URL을 업데이트한다.")
            void updateProfile_success_withNewImage() throws IOException {
                // given
                Long userId = 1L;
                User user = User.builder().id(userId).email("test@example.com").build();
                UserProfile existingUserProfile = UserProfile.builder()
                        .userId(userId)
                        .nickName("oldNickname")
                        .introduce("oldIntroduce")
                        .gender(Gender.Male)
                        .profileImage("old.jpg")
                        .latitude(37.0)
                        .longitude(127.0)
                        .birthdate(LocalDate.of(1990, 1, 1))
                        .user(user)
                        .build();
                ProfileUpdateRequest dto = ProfileUpdateRequest.builder()
                        .nickname("newnickname")
                        .introduce("새로운 소개")
                        .latitude(38.0)
                        .longitude(128.0)
                        .build();
                UserProfile updatedUserProfile = UserProfile.builder()
                        .userId(userId)
                        .nickName("newnickname")
                        .introduce("새로운 소개")
                        .gender(Gender.Male)
                        .profileImage("s3://new.jpg") // 업데이트된 S3 URL
                        .latitude(38.0)
                        .longitude(128.0)
                        .birthdate(LocalDate.of(1990, 1, 1))
                        .user(user)
                        .build();

                MultipartFile mockFile = mock(MultipartFile.class);
                when(mockFile.isEmpty()).thenReturn(false);
                when(s3Util.uploadFile(mockFile)).thenReturn("s3://new.jpg");
                when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingUserProfile));
                when(userProfileRepository.save(any(UserProfile.class))).thenReturn(updatedUserProfile);

                // when
                ProfileResponse responseDto = userProfileService.updateProfile(userId, dto, null);

                // then
                assertThat(responseDto.getNickname()).isEqualTo(dto.getNickname());
                assertThat(responseDto.getIntroduce()).isEqualTo(dto.getIntroduce());
                assertThat(responseDto.getProfileImage()).isEqualTo("s3://new.jpg");
                assertThat(responseDto.getLatitude()).isEqualTo(dto.getLatitude());
                assertThat(responseDto.getLongitude()).isEqualTo(dto.getLongitude());
                assertThat(responseDto.getEmail()).isEqualTo("test@example.com");

                verify(userProfileRepository, times(1)).findByUserId(userId);
                verify(userProfileRepository, times(1)).save(any(UserProfile.class));
                verify(s3Util, never()).uploadFile(any(MultipartFile.class)); // updateProfile 시에는 컨트롤러에서 업로드하므로 여기서는 호출 안됨
            }


        @Nested
        @DisplayName("isNicknameDuplicated 테스트")
        class NicknameDuplicateTest {
            @Test
            @DisplayName("닉네임이 중복인 경우 true를 반환한다.")
            void nicknameDuplicated_true() {
                // given
                String nickname = "duplicateNickname";
                when(userProfileRepository.existsByNickName(nickname)).thenReturn(true);

                // when
                boolean result = userProfileService.isNicknameDuplicated(nickname);

                // then
                assertThat(result).isTrue();
            }

            @Test
            @DisplayName("닉네임이 중복이 아닌 경우 false를 반환한다.")
            void nicknameDuplicated_false() {
                // given
                String nickname = "uniqueNickname";
                when(userProfileRepository.existsByNickName(nickname)).thenReturn(false);

                // when
                boolean result = userProfileService.isNicknameDuplicated(nickname);

                // then
                assertThat(result).isFalse();
            }
        }

        @Test
        @DisplayName("내 프로필을 정상 조회한다.")
        void getMyProfile_success() {
            // given
            Long userId = 1L;

            // User 객체 생성
            User user = User.builder()
                    .id(userId)
                    .email("test@example.com")
                    .name("Test User")
                    .build();

            // UserProfile 객체 생성 시 user 필드 설정
            UserProfile userProfile = UserProfile.builder()
                    .userId(userId)
                    .nickName("nickname")
                    .gender(null)
                    .profileImage("profile.jpg")
                    .latitude(37.5665)
                    .longitude(126.9780)
                    .introduce("소개입니다.")
                    .birthdate(LocalDate.of(2000, 1, 1))
                    .user(user) // ★ user 필드 설정
                    .build();

            when(userProfileRepository.findById(userId)).thenReturn(Optional.of(userProfile));

            // when
            ProfileResponse result = userProfileService.getMyProfile(userId);

            // then
            assertThat(result.getNickname()).isEqualTo("nickname");
            assertThat(result.getIntroduce()).isEqualTo("소개입니다.");
            assertThat(result.getEmail()).isEqualTo("test@example.com"); // User 객체의 email 검증 추가
        }
    }
}