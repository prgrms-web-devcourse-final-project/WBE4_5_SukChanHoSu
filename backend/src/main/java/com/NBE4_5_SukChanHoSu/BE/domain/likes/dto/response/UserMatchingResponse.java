package com.NBE4_5_SukChanHoSu.BE.domain.likes.dto.response;

import com.NBE4_5_SukChanHoSu.BE.domain.likes.entity.Matching;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.UserProfileResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserMatchingResponse {
    private Long matchingId;
    private LocalDateTime createdAt;
    private UserProfileResponse user;

    public UserMatchingResponse(UserProfile user, Matching matching, int distance) {
        this.user = new UserProfileResponse(user, distance);
        this.matchingId = matching.getMatchingId();
        this.createdAt = matching.getCreatedAt();
    }
}