package com.twinsolution.construction.dto;

import lombok.*;

import java.time.LocalDateTime;

public class SettingDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
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
        private Integer chunkSize;
        private Integer chunkOverlap;
        private LocalDateTime updatedAt;
    }
}
