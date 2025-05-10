package com.NBE4_5_SukChanHoSu.BE.domain.movie.review.entity;

import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    // todo 영화 객체로 변경
    private String title;
    private String content;
    private Double rating;

    @CreatedDate
    @Column(updatable = false)
    @JsonProperty("createdAt")
    private LocalDateTime createdDate;

    @LastModifiedDate
    @JsonProperty("modifiedAt")
    private LocalDateTime modifiedDate;

    @JoinColumn(name = "user_id")
    @ManyToOne(fetch = FetchType.EAGER)
    private User user;
}
