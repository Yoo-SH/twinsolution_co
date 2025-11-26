package com.twinsolution.construction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

public class APIDto {

    @Schema(name = "APIRequest", description = "외부 API 등록 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private String name;
        private String baseUrl;
        private String method;
        private String authKey;
        private String status;
    }

    @Schema(name = "APIResponse", description = "외부 API 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String baseUrl;
        private String method;
        private String authKey;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Schema(name = "APIStatusUpdateRequest", description = "API 상태 변경 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusUpdateRequest {
        private String status;
    }

    @Schema(name = "APITestCallResponse", description = "API 테스트 호출 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestCallResponse {
        private String status;
        private Integer statusCode;
        private String message;
        private String responseBody;
    }
}
