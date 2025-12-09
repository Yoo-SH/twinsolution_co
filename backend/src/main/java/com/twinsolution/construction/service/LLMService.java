package com.twinsolution.construction.service;

import com.twinsolution.construction.config.OllamaProperties;
import com.twinsolution.construction.dto.LLMDto;
import com.twinsolution.construction.llm.LLMProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMService {

    private final OllamaProperties ollamaProperties;
    private final LLMProviderFactory llmProviderFactory;

    /**
     * 사용 가능한 모든 LLM Provider와 모델 목록을 반환합니다.
     */
    public LLMDto.ProvidersResponse getAvailableProviders() {
        List<LLMDto.ProviderInfo> providers = new ArrayList<>();

        // OpenAI Provider (항상 사용 가능)
        providers.add(LLMDto.ProviderInfo.builder()
                .name("OpenAI")
                .id("OPENAI")
                .available(true)
                .models(getOpenAIModels())
                .build());

        // Ollama Provider (가용성 체크)
        boolean ollamaAvailable = llmProviderFactory.isOllamaAvailable();
        List<LLMDto.ModelInfo> ollamaModels = ollamaAvailable ? getOllamaModelsInfo() : new ArrayList<>();

        providers.add(LLMDto.ProviderInfo.builder()
                .name("Ollama (로컬)")
                .id("OLLAMA")
                .available(ollamaAvailable)
                .models(ollamaModels)
                .build());

        return LLMDto.ProvidersResponse.builder()
                .providers(providers)
                .build();
    }

    /**
     * Ollama 가용성 및 설치된 모델 목록을 반환합니다.
     */
    public LLMDto.OllamaAvailabilityResponse getOllamaAvailability() {
        boolean available = llmProviderFactory.isOllamaAvailable();
        List<String> models = new ArrayList<>();

        if (available) {
            try {
                List<LLMDto.OllamaModel> ollamaModels = fetchOllamaModels();
                models = ollamaModels.stream()
                        .map(LLMDto.OllamaModel::getName)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Ollama 모델 목록 조회 실패: {}", e.getMessage());
            }
        }

        return LLMDto.OllamaAvailabilityResponse.builder()
                .available(available)
                .models(models)
                .baseUrl(ollamaProperties.getBaseUrl())
                .build();
    }

    /**
     * OpenAI 지원 모델 목록
     */
    private List<LLMDto.ModelInfo> getOpenAIModels() {
        return Arrays.asList(
                LLMDto.ModelInfo.builder()
                        .name("gpt-3.5-turbo")
                        .displayName("GPT-3.5 Turbo")
                        .description("빠르고 효율적인 모델, 일반적인 작업에 적합")
                        .build(),
                LLMDto.ModelInfo.builder()
                        .name("gpt-4")
                        .displayName("GPT-4")
                        .description("더 정확하고 복잡한 추론이 가능한 모델")
                        .build(),
                LLMDto.ModelInfo.builder()
                        .name("gpt-4-turbo")
                        .displayName("GPT-4 Turbo")
                        .description("GPT-4의 빠른 버전, 최신 지식 포함")
                        .build(),
                LLMDto.ModelInfo.builder()
                        .name("gpt-4o")
                        .displayName("GPT-4o")
                        .description("GPT-4의 최적화 버전")
                        .build()
        );
    }

    /**
     * Ollama에서 설치된 모델 목록 조회
     */
    private List<LLMDto.OllamaModel> fetchOllamaModels() {
        try {
            WebClient client = WebClient.create(ollamaProperties.getBaseUrl());

            LLMDto.OllamaTagsResponse response = client.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(LLMDto.OllamaTagsResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null && response.getModels() != null) {
                log.debug("Ollama 모델 목록 조회 성공: {} 개", response.getModels().size());
                return response.getModels();
            }

            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Ollama 모델 목록 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Ollama 모델 정보 목록 변환
     */
    private List<LLMDto.ModelInfo> getOllamaModelsInfo() {
        List<LLMDto.OllamaModel> ollamaModels = fetchOllamaModels();

        return ollamaModels.stream()
                .map(model -> LLMDto.ModelInfo.builder()
                        .name(model.getName())
                        .displayName(model.getName())
                        .description(buildOllamaModelDescription(model))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Ollama 모델 설명 생성
     */
    private String buildOllamaModelDescription(LLMDto.OllamaModel model) {
        StringBuilder desc = new StringBuilder();

        if (model.getDetails() != null) {
            if (model.getDetails().getParameterSize() != null) {
                desc.append(model.getDetails().getParameterSize()).append(" 파라미터");
            }
            if (model.getDetails().getFamily() != null) {
                if (desc.length() > 0) desc.append(", ");
                desc.append(model.getDetails().getFamily()).append(" 계열");
            }
            if (model.getDetails().getQuantizationLevel() != null) {
                if (desc.length() > 0) desc.append(", ");
                desc.append(model.getDetails().getQuantizationLevel()).append(" 양자화");
            }
        }

        if (model.getSize() != null) {
            if (desc.length() > 0) desc.append(", ");
            desc.append(formatFileSize(model.getSize()));
        }

        return desc.length() > 0 ? desc.toString() : "로컬 모델";
    }

    /**
     * 파일 크기 포맷팅
     */
    private String formatFileSize(Long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
