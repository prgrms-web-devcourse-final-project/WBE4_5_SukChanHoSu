package com.NBE4_5_SukChanHoSu.BE.domain.likes;


import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.UserProfile;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(name = "user_likes")
public class UserLikes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userLikeId;

    @ManyToOne
    @JoinColumn(name = "liker_id", nullable = false)
    @JsonBackReference
    private UserProfile fromUser;  // 좋아요를 보낸 사용자

    @ManyToOne
    @JoinColumn(name = "liked_id", nullable = false)
    @JsonBackReference
    private UserProfile toUser;  // 좋아요를 받은 사용자

    @Column(name = "like_time",nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date likeTime;

    public UserLikes(UserProfile fromUser, UserProfile toUser) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.likeTime = new Date();
    }
}
