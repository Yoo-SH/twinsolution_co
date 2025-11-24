package com.twinsolution.construction.dto;

import lombok.*;

import java.time.LocalDateTime;

public class AnalysisReportDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private Long documentId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long documentId;
        private String content;
        private LocalDateTime createdAt;
    }
}
