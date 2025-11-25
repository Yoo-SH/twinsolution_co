package com.twinsolution.construction.dto;

import lombok.*;

import java.time.LocalDateTime;

public class DocumentDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private Long projectId;
        private String name;
        private String fileType;
        private String filePath;
        private String origin;
        private String status;
        private Integer chunkSize;
        private Integer chunkOverlap;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long projectId;
        private String name;
        private String fileType;
        private String filePath;
        private String origin;
        private String status;
        private Integer chunkSize;
        private Integer chunkOverlap;
        private Long chunkCount;
        private Boolean hasAnalysisReport;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UploadRequest {
        private Long projectId;
        private String name;
        private String fileType;
    }
}
