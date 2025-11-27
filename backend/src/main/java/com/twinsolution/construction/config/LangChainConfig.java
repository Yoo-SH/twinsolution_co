package com.twinsolution.construction.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class LangChainConfig {

    private final OpenAiProperties properties;

    /**
     * LangChain4j용 일반 Chat Model 빈 생성
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getModel())
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxTokens())
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * LangChain4j용 스트리밍 Chat Model 빈 생성
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(properties.getApiKey())
                .modelName(properties.getModel())
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxTokens())
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 세션별 ChatMemory를 관리하는 맵
     * 실제 프로덕션에서는 Redis 등 외부 저장소 사용 권장
     */
    @Bean
    public Map<Long, ChatMemory> chatMemoryStore() {
        return new HashMap<>();
    }

    /**
     * 새로운 ChatMemory를 생성하는 팩토리 메서드
     * @param maxMessages 메모리에 유지할 최대 메시지 수
     */
    public ChatMemory createChatMemory(int maxMessages) {
        return MessageWindowChatMemory.withMaxMessages(maxMessages);
    }
}
