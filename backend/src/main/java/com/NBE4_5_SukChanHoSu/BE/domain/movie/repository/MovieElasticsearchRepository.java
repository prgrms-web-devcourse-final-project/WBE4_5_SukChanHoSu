package com.NBE4_5_SukChanHoSu.BE.domain.movie.repository;

import com.NBE4_5_SukChanHoSu.BE.domain.movie.document.MovieDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieElasticsearchRepository extends ElasticsearchRepository<MovieDocument, Long> {
    // 자동완성을 위한 메서드 (예시 - 제목 prefix 검색)
    List<MovieDocument> findByTitleStartingWith(String title); // movieNm 자동완성
}
