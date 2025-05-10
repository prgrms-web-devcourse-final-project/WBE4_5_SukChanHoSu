package com.NBE4_5_SukChanHoSu.BE.domain.recommend.service;

import com.NBE4_5_SukChanHoSu.BE.domain.likes.repository.UserLikesRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.UserProfileResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.Genre;
import com.NBE4_5_SukChanHoSu.BE.domain.recommend.entity.RecommendUser;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserProfile;
import com.NBE4_5_SukChanHoSu.BE.domain.recommend.repository.RecommendUserRepository;
import com.NBE4_5_SukChanHoSu.BE.domain.user.repository.UserProfileRepository;
import com.NBE4_5_SukChanHoSu.BE.global.exception.user.NoRecommendException;
import com.NBE4_5_SukChanHoSu.BE.global.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendService {
    private final UserProfileRepository userProfileRepository;
    private final RecommendUserRepository recommendUserRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserLikesRepository userLikesRepository;

    static final double EARTH_RADIUS = 6371; // 지구의 반지름 (단위: km)

    public UserProfile findUser(Long userId) {
        return userProfileRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("401", "존재하지 않는 유저입니다."));
    }

    // 이성만 조회
    public List<UserProfile> findProfileByGender(UserProfile userProfile) {
        return userProfileRepository.findAll().stream().filter(profile -> !profile.getUserId().equals(userProfile.getUserId())) // 자신 제외
                .filter(profile -> !profile.getGender().equals(userProfile.getGender())) // 성별이 다른 유저 필터링
                .toList();
    }

    // 거리 내에 존재하는 사용자 리스트
    public List<UserProfileResponse> findProfileWithinRadius(UserProfile userProfile, Integer radius) {
        // 이성으로 필터링
        List<UserProfile> profileByGender = findProfileByGender(userProfile);

        // 거리 계산
        List<UserProfileResponse> responses = new ArrayList<>();
        for (UserProfile profile : profileByGender) {
            int distance = calDistance(userProfile, profile); // 거리 계산
            if (distance <= radius) { // 거리가 범위 이내인 경우만 추가
                responses.add(new UserProfileResponse(profile, distance));
            }
        }
        return responses;
    }

    // 범위 내에 존재하는 사용자 추천
    @Transactional
    public UserProfileResponse recommendByDistance(UserProfile userProfile, Integer distance) {
        Long userId = userProfile.getUserId();

        // 범위 이내에 존재하는 사용자 리스트
        List<UserProfileResponse> list = findProfileWithinRadius(userProfile, distance);

        // 이미 추천한 사용자 제외
        List<UserProfileResponse> candidates = list.stream()
                .filter(candidate -> !isRecommended(userId,candidate.getUserId(),"distance"))
                .toList();

        // 남아있는 사용자 있는 경우 랜덤 추천
        if(!candidates.isEmpty()){
            Random random = new Random();
            UserProfileResponse recommendedUser = candidates.get(random.nextInt(candidates.size()));

            // 객체화 하여 DB에 저장
            saveRecommendUser(userId,recommendedUser.getUserId(),"distance");
            return recommendedUser;
        }

        throw new NoRecommendException("404", "추천할 사용자가 없습니다.");
    }

    // 거리 계산
    public int calDistance(UserProfile userProfile1, UserProfile userProfile2) {
        // 내 위/경도
        double lat1 = userProfile1.getLatitude();
        double lon1 = userProfile1.getLongitude();
        // 탐색 대상의 위/경도
        double lat2 = userProfile2.getLatitude();
        double lon2 = userProfile2.getLongitude();

        // 위/경도 차이
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // 곡률 감안해서 어쩌구저쩌구해서 거리 계산
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 거리를 km로 계산
        double distance = EARTH_RADIUS * c;

        // 소수 첫째 자리에서 반올림 후 int형으로 반환
        return (int) Math.round(distance); // 반올림하여 int형으로 반환
    }

    // 태그 기반 매칭
    @Transactional
    public UserProfileResponse recommendUserByTags(UserProfile userProfile) {
        long userId = userProfile.getUserId();
        List<Genre> tags = userProfile.getFavoriteGenres();
        int maxScore = -1;
        int radius = userProfile.getSearchRadius();
        int recommendDistance = 0;
        UserProfile recommendedUser = null;

        // 1차: 이성
        List<UserProfile> profileByGender = findProfileByGender(userProfile);

        // 거리 및 태그
        for (UserProfile profile : profileByGender) {
            // 이미 추천한 사용자 pass
            if(isRecommended(userId, profile.getUserId(),"tags")) continue;
            // 거리 계산
            int distance = calDistance(userProfile, profile);
            // 범위 밖 사용자 패스
            if (distance > radius) continue;

            // 겹치는 태그 계산
            int count = countCommonTags(profile, tags);
            if(count > maxScore) {
                maxScore = count;
                recommendDistance = distance;
                recommendedUser = profile;
            }
        }

        // 추천할 사용자가 있는 경우
        if(recommendedUser != null) {
            // 객체화 하여 DB에 저장
            saveRecommendUser(userId,recommendedUser.getUserId(),"tags");
            return new UserProfileResponse(recommendedUser,recommendDistance);
        }

        throw new NoRecommendException("404","추천할 사용자가 없습니다.");
    }

    // 추천 리스트 저장
    private void saveRecommendUser(long userId, Long recommendedUserId, String type) {
        RecommendUser recommendUser = new RecommendUser(userId,recommendedUserId,type);
        recommendUserRepository.save(recommendUser);
    }

    // 이미 추천한 사용지인지 검증
    private boolean isRecommended(long userId, Long recommendedUserId, String type) {
        return recommendUserRepository.existsByUserIdAndRecommendedUserIdAndType(userId,recommendedUserId,type);
    }

    // 두 사용자의 겹치는 태그 수 계산
    private int countCommonTags(UserProfile user, List<Genre> tags) {
        return (int) user.getFavoriteGenres().stream().filter(tags::contains).count();
    }

    // 영화로 추천
    @Transactional
    public UserProfileResponse recommendUserByMovie(UserProfile profile, String movieCd) {
        String pattern = "user:*";
        Set<String> keys = redisTemplate.keys(pattern); // 패턴에 해당하는 키 탐색
        UserProfile recommendedUser = null;
        int radius = profile.getSearchRadius();
        int distance = 0;

        // 레디스의 사용자 필터링: 1순위(영화 겹치는 사용자) + 2순위(안겹치는 사용자)
        List<UserProfile> filteredUser = filterFromRedis(keys,profile,distance,radius,movieCd);

        // 레디스에 저장된 사용자 우선 반환
        if(!filteredUser.isEmpty()){
            recommendedUser = getRandomUser(filteredUser);
            saveRecommendUser(profile.getUserId(), recommendedUser.getUserId(),"movie");
            return new UserProfileResponse(recommendedUser,distance);
        }

        // 3순위: DB에 저장된 사용자
        List<UserProfile> dbUsers = filterFromDb(profile,recommendedUser,distance,radius,movieCd);
        if(!dbUsers.isEmpty()) {
            recommendedUser = getRandomUser(dbUsers);
            saveRecommendUser(profile.getUserId(), recommendedUser.getUserId(),"movie");
            return new UserProfileResponse(recommendedUser,distance);
        }

        // 없음
        throw new NoRecommendException("404","추천할 사용자가 없습니다.");
    }

    private List<UserProfile> filterFromDb(UserProfile profile,UserProfile recommendedUser, int distance, int radius, String movieCd) {
        // 이성 사용자
        List<UserProfile> profilesByGender = findProfileByGender(profile);
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

        // 3순위 사용자 저장할 리스트
        List<UserProfile> candidates = new ArrayList<>();
        // 3순위: 레디스에 저장되지 않은 사용자들 중에서 추천
        for(UserProfile profile2 : profilesByGender) {
            // 1달 이내 사용자만 조건 목록 포함
            LocalDateTime lastLikeTime = userLikesRepository.findLastLikeTimeByUserId(profile2.getUserId());
            if (lastLikeTime == null || lastLikeTime.isBefore(oneMonthAgo)) continue;

            // 중복 추천 방지
            if(isRecommended(profile.getUserId(), profile2.getUserId(), "movie")) continue;

            //  범위 밖 사용자 제외
            distance = calDistance(profile, profile2);
            if(radius < distance) continue;

            candidates.add(profile2);
        }

        return candidates;
    }

    private List<UserProfile> filterFromRedis(Set<String> keys, UserProfile profile, int distance, int radius, String movieCd) {
        // 영화가 겹치는 사용자 목록
        List<UserProfile> matchingMovieUsers = new ArrayList<>();
        // 영화가 겹치지 않는 사용자 목록
        List<UserProfile> nonMatchingMovieUsers = new ArrayList<>();

        // 1. 레디스에 저장된 사람들 우선 추천(마지막 활동이 1주일 이내인 사람들)
        if(keys != null) {
            for(String key:keys){
                Long userId = Long.valueOf(key.split(":")[1]);  // Id 추출
                UserProfile profile2 = userProfileRepository.findById(userId)
                        .orElseThrow(() -> new UserNotFoundException("401","존재하지 않는 유저입니다."));

                // 2. 동성 제외
                if(profile.getGender().equals(profile2.getGender())) continue;

                // 3. 중복 추천 방지
                if(isRecommended(profile.getUserId(), userId, "movie")) continue;

                // 4. 범위 밖 사용자 제외
                distance = calDistance(profile, profile2);
                if(radius < distance) continue;

                String value = (String) redisTemplate.opsForValue().get(key);   // movieCd 추출
                // 5. 보고싶은 영화가 겹치는 사람들 우선 탐색
                if(movieCd.equals(value)) {
                    // user:3 -> 3
                    matchingMovieUsers.add(profile2);
                }else{
                    // 6. 보고싶은 영화가 겹치는 사람이 없을 경우에 후순위 추천
                    nonMatchingMovieUsers.add(profile2);
                }
            }
        }

        // 겹치는 사용자가 있으면 겹치는 사용자 목록 반환, 겹치는 사용자가 없으면 일반 사용자 반환
        return matchingMovieUsers.isEmpty() ? nonMatchingMovieUsers : matchingMovieUsers;
    }

    // 우선순위가 같은 유저에서 랜덤 선택
    private UserProfile getRandomUser(List<UserProfile> userProfiles) {
        Random random = new Random();
        return userProfiles.get(random.nextInt(userProfiles.size()));
    }

}
