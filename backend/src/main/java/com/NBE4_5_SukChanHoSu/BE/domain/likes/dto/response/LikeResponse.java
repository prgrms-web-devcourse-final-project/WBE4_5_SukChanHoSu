package com.NBE4_5_SukChanHoSu.BE.domain.likes.dto.response;

import com.NBE4_5_SukChanHoSu.BE.domain.likes.entity.UserLikes;
import com.NBE4_5_SukChanHoSu.BE.domain.user.dto.response.UserProfileResponse;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {
    private Long userLikeId;
    private UserProfileResponse toUser;
    private Date likeTime;

    public LikeResponse(UserLikes userLikes, UserProfile userProfile,int radius) {
        this.userLikeId = userLikes.getUserLikeId();
        this.toUser = new UserProfileResponse(userProfile,radius);
        this.likeTime = userLikes.getLikeTime();
    }

}
