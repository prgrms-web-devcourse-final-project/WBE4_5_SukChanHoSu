package com.NBE4_5_SukChanHoSu.BE.domain.likes.dto.response;

import com.NBE4_5_SukChanHoSu.BE.domain.likes.entity.Matching;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.UserProfileResponse;
import lombok.Data;

import java.util.Date;

@Data
public class MatchingResponse {
    private Long matchingId;
    private UserProfileResponse maleUser;
    private UserProfileResponse femaleUser;
    private Date matchingTime;

    public MatchingResponse(Matching matching, int distance) {
        this.matchingId = matching.getMatchingId();
        this.maleUser = new UserProfileResponse(matching.getMaleUser(), distance);
        this.femaleUser = new UserProfileResponse(matching.getFemaleUser(), distance);
        this.matchingTime = matching.getMatchingTime();
    }
}