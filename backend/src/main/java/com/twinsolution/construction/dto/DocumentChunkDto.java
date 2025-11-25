package com.twinsolution.construction.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class DocumentChunkDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long documentId;
        private Integer index;
        private String content;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PreviewRequest {
        private String text;
        private Integer chunkSize;
        private Integer chunkOverlap;
        private List<String> separators;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PreviewResponse {
        private List<ChunkPreview> chunks;
        private Integer totalChunks;
        private Double averageLength;
        private Double overlapRatio;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChunkPreview {
        private Integer index;
        private String content;
        private Integer length;
    }
}
