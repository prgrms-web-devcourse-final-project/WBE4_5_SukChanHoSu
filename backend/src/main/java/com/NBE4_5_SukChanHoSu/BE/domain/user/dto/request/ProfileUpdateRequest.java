package com.NBE4_5_SukChanHoSu.BE.domain.user.dto.request;

import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.Gender;
import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.Genre;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ProfileUpdateRequest {

    private String nickname;
    private Gender gender;
    private Double latitude;
    private Double longitude;
    private LocalDate birthdate;
    private int searchRadius;
    private String lifeMovie;
    private List<Genre> favoriteGenres;
    private List<String> watchedMovies;
    private List<String> preferredTheaters;
    private String introduce;

}
