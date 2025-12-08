package com.twinsolution.construction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class RagDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentUploadResponse {
        private Boolean success;
        private String message;

        @JsonProperty("document_count")
        private Integer documentCount;

        @JsonProperty("chunk_count")
        private Integer chunkCount;

        @JsonProperty("indexed_ids")
        private List<String> indexedIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchRequest {
        private String query;
        private String projectId;  // 프로젝트 ID 필터
        private Integer topK;
        private Float similarityThreshold;
        private Boolean enableReranking;
        private String filterSource;
        private Integer maxContextLength;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResponse {
        private Boolean success;
        private String query;
        private List<SearchResult> results;
        private String context;
        private List<String> contexts;
        private Integer totalResults;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private String id;
        private String content;
        private Map<String, Object> metadata;
        private Float distance;
        private Float similarity;
    }
}
