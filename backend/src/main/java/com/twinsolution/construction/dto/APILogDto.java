package com.twinsolution.construction.dto;

import lombok.*;

import java.time.LocalDateTime;

public class APILogDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long apiId;
        private String status;
        private Integer statusCode;
        private String message;
        private LocalDateTime createdAt;
    }
}
