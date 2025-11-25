package com.twinsolution.construction.dto;

import lombok.*;

import java.time.LocalDateTime;

public class ChatMessageDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long sessionId;
        private String role;
        private String content;
        private LocalDateTime createdAt;
    }
}
