package com.twinsolution.construction.llm;

import com.twinsolution.construction.config.OllamaProperties;
import com.twinsolution.construction.config.OpenAiProperties;
import com.twinsolution.construction.entity.Project;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LLM Provider Factory 구현체
 * OpenAI와 Ollama 모델을 관리하고 제공합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LLMProviderFactoryImpl implements LLMProviderFactory {

    private final OpenAiProperties openAiProperties;
    private final OllamaProperties ollamaProperties;

    // 모델 인스턴스 캐시 (메모리 효율성을 위해)
    private final Map<String, ChatLanguageModel> chatModelCache = new ConcurrentHashMap<>();
    private final Map<String, StreamingChatLanguageModel> streamingModelCache = new ConcurrentHashMap<>();

    // Ollama 가용성 상태 캐시
    private volatile Boolean ollamaAvailable = null;
    private volatile long lastOllamaCheck = 0;
    private static final long OLLAMA_CHECK_INTERVAL = 60000; // 1분

    @Override
    public ChatLanguageModel getChatModel(Project project) {
        String provider = project.getLlmProvider();
        String modelName = project.getModelName();
        return getChatModel(provider, modelName);
    }

    @Override
    public ChatLanguageModel getChatModel(String llmProvider, String modelName) {
        // null/empty 체크 및 기본값 설정
        String provider = (llmProvider != null && !llmProvider.isEmpty()) ? llmProvider : "OPENAI";
        String model = (modelName != null && !modelName.isEmpty()) ? modelName : openAiProperties.getModel();

        // OLLAMA 선택 시, 사용 불가하면 OpenAI로 fallback
        if ("OLLAMA".equalsIgnoreCase(provider)) {
            if (isOllamaAvailable()) {
                return getOllamaChatModel(model);
            } else {
                log.warn("Ollama가 사용 불가합니다. OpenAI로 fallback합니다.");
                return getOpenAiChatModel(openAiProperties.getModel());
            }
        }

        // 기본값: OpenAI
        return getOpenAiChatModel(model);
    }

    @Override
    public StreamingChatLanguageModel getStreamingChatModel(Project project) {
        String provider = project.getLlmProvider();
        String modelName = project.getModelName();
        return getStreamingChatModel(provider, modelName);
    }

    @Override
    public StreamingChatLanguageModel getStreamingChatModel(String llmProvider, String modelName) {
        // null/empty 체크 및 기본값 설정
        String provider = (llmProvider != null && !llmProvider.isEmpty()) ? llmProvider : "OPENAI";
        String model = (modelName != null && !modelName.isEmpty()) ? modelName : openAiProperties.getModel();

        // OLLAMA 선택 시, 사용 불가하면 OpenAI로 fallback
        if ("OLLAMA".equalsIgnoreCase(provider)) {
            if (isOllamaAvailable()) {
                return getOllamaStreamingChatModel(model);
            } else {
                log.warn("Ollama가 사용 불가합니다. OpenAI로 fallback합니다.");
                return getOpenAiStreamingChatModel(openAiProperties.getModel());
            }
        }

        // 기본값: OpenAI
        return getOpenAiStreamingChatModel(model);
    }

    @Override
    public boolean isOllamaAvailable() {
        long now = System.currentTimeMillis();

        // 캐시된 결과 사용 (1분 이내)
        if (ollamaAvailable != null && (now - lastOllamaCheck) < OLLAMA_CHECK_INTERVAL) {
            return ollamaAvailable;
        }

        // Ollama 서비스 체크
        try {
            WebClient client = WebClient.create(ollamaProperties.getBaseUrl());
            String response = client.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            ollamaAvailable = response != null;
            lastOllamaCheck = now;
            log.debug("Ollama 가용성 체크: {}", ollamaAvailable);
            return ollamaAvailable;

        } catch (Exception e) {
            log.debug("Ollama 서비스 연결 실패: {}", e.getMessage());
            ollamaAvailable = false;
            lastOllamaCheck = now;
            return false;
        }
    }

    /**
     * OpenAI ChatModel 인스턴스 가져오기 (캐시 사용)
     */
    private ChatLanguageModel getOpenAiChatModel(String modelName) {
        String cacheKey = "openai:" + modelName;
        return chatModelCache.computeIfAbsent(cacheKey, key -> {
            log.info("OpenAI ChatModel 생성: {}", modelName);
            return OpenAiChatModel.builder()
                    .apiKey(openAiProperties.getApiKey())
                    .modelName(modelName)
                    .temperature(openAiProperties.getTemperature())
                    .maxTokens(openAiProperties.getMaxTokens())
                    .timeout(Duration.ofSeconds(60))
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        });
    }

    /**
     * OpenAI StreamingChatModel 인스턴스 가져오기 (캐시 사용)
     */
    private StreamingChatLanguageModel getOpenAiStreamingChatModel(String modelName) {
        String cacheKey = "openai:" + modelName;
        return streamingModelCache.computeIfAbsent(cacheKey, key -> {
            log.info("OpenAI StreamingChatModel 생성: {}", modelName);
            return OpenAiStreamingChatModel.builder()
                    .apiKey(openAiProperties.getApiKey())
                    .modelName(modelName)
                    .temperature(openAiProperties.getTemperature())
                    .maxTokens(openAiProperties.getMaxTokens())
                    .timeout(Duration.ofSeconds(60))
                    .logRequests(true)
                    .logResponses(true)
                    .build();
        });
    }

    /**
     * Ollama ChatModel 인스턴스 가져오기 (캐시 사용)
     */
    private ChatLanguageModel getOllamaChatModel(String modelName) {
        String cacheKey = "ollama:" + modelName;
        return chatModelCache.computeIfAbsent(cacheKey, key -> {
            log.info("Ollama ChatModel 생성: {}", modelName);
            return OllamaChatModel.builder()
                    .baseUrl(ollamaProperties.getBaseUrl())
                    .modelName(modelName)
                    .temperature(0.7)
                    .timeout(Duration.ofSeconds(ollamaProperties.getTimeout()))
                    .build();
        });
    }

    /**
     * Ollama StreamingChatModel 인스턴스 가져오기 (캐시 사용)
     */
    private StreamingChatLanguageModel getOllamaStreamingChatModel(String modelName) {
        String cacheKey = "ollama:" + modelName;
        return streamingModelCache.computeIfAbsent(cacheKey, key -> {
            log.info("Ollama StreamingChatModel 생성: {}", modelName);
            return OllamaStreamingChatModel.builder()
                    .baseUrl(ollamaProperties.getBaseUrl())
                    .modelName(modelName)
                    .temperature(0.7)
                    .timeout(Duration.ofSeconds(ollamaProperties.getTimeout()))
                    .build();
        });
    }

    /**
     * 캐시 초기화 (설정 변경 시 호출)
     */
    public void clearCache() {
        chatModelCache.clear();
        streamingModelCache.clear();
        ollamaAvailable = null;
        log.info("LLM 모델 캐시 초기화 완료");
    }
}
