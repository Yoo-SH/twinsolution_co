package com.twinsolution.construction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

public class OpenAiDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatRequest {
        private String model;
        private List<Message> messages;
        private Double temperature;
        private Integer maxTokens;
        private Boolean stream;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        private String role;
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatResponse {
        private String id;
        private String object;
        private Long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Choice {
        private Integer index;
        private Message message;
        private String finishReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }

    // 스트리밍 응답 DTO
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StreamResponse {
        private String id;
        private String object;
        private Long created;
        private String model;
        private List<StreamChoice> choices;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StreamChoice {
        private Integer index;
        private Delta delta;
        private String finishReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Delta {
        private String role;
        private String content;
    }

    // 통합 채팅 요청 DTO
    @Schema(description = "OpenAI 채팅 API 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatApiRequest {
        @Schema(description = "사용자 메시지 (필수)", example = "안녕하세요, 건설 프로젝트에 대해 질문이 있습니다.", required = true)
        private String message;

        @Schema(description = "시스템 프롬프트 (AI의 역할 및 행동 지시, 선택사항)",
                example = "당신은 건설 전문가입니다. 건설, 건축 자재, 안전 규정, 프로젝트 관리에 대한 정확하고 실용적인 답변을 제공하세요.")
        private String systemPrompt;

        @Schema(description = "사용할 OpenAI 모델 (선택사항, 기본값: gpt-3.5-turbo)",
                example = "gpt-4",
                allowableValues = {"gpt-3.5-turbo", "gpt-4", "gpt-4-turbo", "gpt-4o"})
        private String model;

        @Schema(description = "Temperature (0.0~2.0, 낮을수록 일관적, 높을수록 창의적, 선택사항)",
                example = "0.7",
                minimum = "0.0",
                maximum = "2.0")
        private Double temperature;

        @Schema(description = "최대 토큰 수 (선택사항)", example = "1000")
        private Integer maxTokens;

        @Schema(description = "스트리밍 모드 활성화 여부 (선택사항, 기본값: false)", example = "false")
        private Boolean stream;
    }

    // 통합 채팅 응답 DTO
    @Schema(description = "OpenAI 채팅 API 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatApiResponse {
        @Schema(description = "AI 응답 메시지", example = "안녕하세요! 건설 프로젝트에 대해 도와드리겠습니다. 어떤 질문이 있으신가요?")
        private String response;

        @Schema(description = "사용된 모델", example = "gpt-3.5-turbo")
        private String model;

        @Schema(description = "총 사용된 토큰 수", example = "150")
        private Integer totalTokens;

        @Schema(description = "프롬프트 토큰 수", example = "50")
        private Integer promptTokens;

        @Schema(description = "응답 생성에 사용된 토큰 수", example = "100")
        private Integer completionTokens;
    }
}
