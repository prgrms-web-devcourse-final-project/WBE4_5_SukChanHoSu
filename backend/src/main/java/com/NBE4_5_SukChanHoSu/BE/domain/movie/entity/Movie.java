package com.NBE4_5_SukChanHoSu.BE.domain.movie.entity;

import com.NBE4_5_SukChanHoSu.BE.domain.user.entity.Genre;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Movie {

    @Id
    @Column(name = "movieCd", nullable = false, length = 20)
    private Long movieId; // = movieCd

    @Column(name = "genres")
    private String genresRaw; // = genres

    @Transient
    @Builder.Default
    private List<Genre> genres = new ArrayList<>(); // 로직에서 사용

    @Column(name = "movieNm", nullable = false)
    private String title; // = movieNm

    @Column(name = "openDt")
    private String releaseDate; // = openDt 데이터 파싱 필요

    @Column(name = "posterUrl", length = 1000)
    private String posterImage; // = posterUrl

    @Column(name = "overview", columnDefinition = "TEXT")
    private String description; // = overview

    @Column(name = "rating")
    private String rating; // = 미지정

    @Column(name = "director", length = 100)
    private String director; // = director

    @PostLoad
    public void loadGenresFromRaw() {
        if (this.genresRaw != null && !this.genresRaw.isBlank()) {
            this.genres = Arrays.stream(genresRaw.split(","))
                    .map(String::trim)
                    .map(Genre::fromLabel)
                    .collect(Collectors.toList());
            System.out.println("🎯 genres: " + this.genres);
        } else {
            this.genres = new ArrayList<>();
        }
    }
}