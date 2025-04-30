package com.NBE4_5_SukChanHoSu.BE.global;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@Getter
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
public class BaseTime {

    @CreatedDate
    @Setter(AccessLevel.PRIVATE)
    @JsonProperty("createdAt")
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Setter(AccessLevel.PRIVATE)
    @JsonProperty("modifiedAt")
    private LocalDateTime modifiedDate;
}
