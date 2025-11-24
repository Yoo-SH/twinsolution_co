package com.twinsolution.construction.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class ChatSessionDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private Long projectId;
        private String quickQuestion;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long projectId;
        private String quickQuestion;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuickQuestionRequest {
        private String quickQuestion;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeneratedDocumentRequest {
        private List<DocumentItem> documents;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentItem {
        private String name;
        private String fileType;
        private String content;
    }
}
