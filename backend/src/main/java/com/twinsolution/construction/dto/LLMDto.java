package com.twinsolution.construction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

public class LLMDto {

    @Schema(name = "LLMProvidersResponse", description = "사용 가능한 LLM Provider 목록")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProvidersResponse {
        @Schema(description = "Provider 목록")
        private List<ProviderInfo> providers;
    }

    @Schema(name = "ProviderInfo", description = "LLM Provider 정보")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProviderInfo {
        @Schema(description = "Provider 이름", example = "OpenAI")
        private String name;

        @Schema(description = "Provider ID", example = "OPENAI")
        private String id;

        @Schema(description = "사용 가능 여부")
        private boolean available;

        @Schema(description = "지원 모델 목록")
        private List<ModelInfo> models;
    }

    @Schema(name = "ModelInfo", description = "LLM 모델 정보")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModelInfo {
        @Schema(description = "모델 이름", example = "gpt-3.5-turbo")
        private String name;

        @Schema(description = "모델 표시 이름", example = "GPT-3.5 Turbo")
        private String displayName;

        @Schema(description = "모델 설명")
        private String description;
    }

    @Schema(name = "OllamaAvailabilityResponse", description = "Ollama 가용성 응답")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OllamaAvailabilityResponse {
        @Schema(description = "Ollama 사용 가능 여부")
        private boolean available;

        @Schema(description = "설치된 모델 목록")
        private List<String> models;

        @Schema(description = "Ollama 서버 URL")
        private String baseUrl;
    }

    /**
     * Ollama API /api/tags 응답 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OllamaTagsResponse {
        private List<OllamaModel> models;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OllamaModel {
        private String name;
        private String model;

        @JsonProperty("modified_at")
        private String modifiedAt;

        private Long size;
        private String digest;
        private OllamaModelDetails details;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OllamaModelDetails {
        @JsonProperty("parent_model")
        private String parentModel;

        private String format;
        private String family;
        private List<String> families;

        @JsonProperty("parameter_size")
        private String parameterSize;

        @JsonProperty("quantization_level")
        private String quantizationLevel;
    }
}
