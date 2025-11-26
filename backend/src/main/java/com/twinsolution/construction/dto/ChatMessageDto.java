package com.twinsolution.construction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

public class ChatMessageDto {

    @Schema(name = "ChatMessageRequest", description = "채팅 메시지 생성 요청")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @Schema(description = "사용자 메시지 (필수)", example = "안녕하세요, 건설 프로젝트에 대해 질문이 있습니다.", required = true)
        private String content;

        @Schema(description = "시스템 프롬프트 (AI의 역할 및 행동 지시, 선택사항)",
                example = "당신은 건설 전문가입니다. 건설, 건축 자재, 안전 규정, 프로젝트 관리에 대한 정확하고 실용적인 답변을 제공하세요.")
        private String systemPrompt;

        @Schema(description = "사용할 OpenAI 모델 (선택사항, 기본값: gpt-3.5-turbo)",
                example = "gpt-3.5-turbo",
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

    @Schema(name = "ChatMessageResponse", description = "채팅 메시지 응답")
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
