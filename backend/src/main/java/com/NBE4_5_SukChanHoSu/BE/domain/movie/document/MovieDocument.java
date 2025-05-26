package com.NBE4_5_SukChanHoSu.BE.domain.movie.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "movies")
@Setting(settingPath = "/elasticsearch/settings/autocomplete.json") // 자동완성 설정 (선택 사항)
public class MovieDocument {

    @Id
    private Long movieId;

    @Field(type = FieldType.Text, analyzer = "autocomplete_analyzer", searchAnalyzer = "autocomplete_search_analyzer")
    private String title; // movieNm

    @Field(type = FieldType.Text, analyzer = "autocomplete_analyzer", searchAnalyzer = "autocomplete_search_analyzer")
    private String genresRaw; // genres

    @Field(type = FieldType.Text, analyzer = "autocomplete_analyzer", searchAnalyzer = "autocomplete_search_analyzer")
    private String description; // openDt 데이터 필요 (추후 활성화 시)

    @Field(type = FieldType.Text, analyzer = "autocomplete_analyzer", searchAnalyzer = "autocomplete_search_analyzer")
    private String director; // openDt 데이터 필요 (추후 활성화 시)

    // 필요한 다른 필드들 (releaseDate, posterImage, rating 등 필요에 따라 추가)
}