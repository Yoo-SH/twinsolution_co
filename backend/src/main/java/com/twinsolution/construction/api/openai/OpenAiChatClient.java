package com.twinsolution.construction.api.openai;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.twinsolution.construction.config.OpenAiProperties;
import com.twinsolution.construction.dto.OpenAiDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class OpenAiChatClient {

    private final OpenAiProperties properties;
    private final WebClient webClient;

    public OpenAiChatClient(OpenAiProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getApiUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 기본 모델(gpt-3.5-turbo)을 사용하여 채팅 메시지 전송
     */
    public OpenAiDto.ChatResponse sendMessage(List<OpenAiDto.Message> messages) {
        return sendMessage(messages, properties.getModel());
    }

    /**
     * 모델을 선택하여 채팅 메시지 전송
     * @param messages 채팅 메시지 리스트
     * @param model 사용할 OpenAI 모델 (예: "gpt-3.5-turbo", "gpt-4", "gpt-4-turbo-preview")
     */
    public OpenAiDto.ChatResponse sendMessage(List<OpenAiDto.Message> messages, String model) {
        return sendMessage(messages, model, properties.getTemperature(), properties.getMaxTokens());
    }

    /**
     * 모든 옵션을 커스터마이징하여 채팅 메시지 전송
     * @param messages 채팅 메시지 리스트
     * @param model 사용할 OpenAI 모델
     * @param temperature 랜덤성 조절 (0.0 ~ 2.0)
     * @param maxTokens 응답의 최대 토큰 수
     */
    public OpenAiDto.ChatResponse sendMessage(
            List<OpenAiDto.Message> messages,
            String model,
            Double temperature,
            Integer maxTokens) {

        try {
            // 요청 본문 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model != null ? model : properties.getModel());
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature != null ? temperature : properties.getTemperature());
            requestBody.put("max_tokens", maxTokens != null ? maxTokens : properties.getMaxTokens());

            log.info("OpenAI 요청 전송 중, 모델: {}", model);

            // 요청 전송 및 응답 수신
            Mono<String> responseMono = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class);

            String responseJson = responseMono.block();
            log.debug("OpenAI 응답: {}", responseJson);

            // 응답 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            OpenAiDto.ChatResponse response = objectMapper.readValue(responseJson, OpenAiDto.ChatResponse.class);

            log.info("OpenAI 요청 완료. 사용된 토큰: {}",
                    response.getUsage() != null ? response.getUsage().getTotalTokens() : "알 수 없음");

            return response;

        } catch (Exception e) {
            log.error("OpenAI API 호출 오류: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI API 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 단일 사용자 메시지를 간단하게 전송하는 헬퍼 메서드
     */
    public String sendSimpleMessage(String userMessage) {
        return sendSimpleMessage(userMessage, properties.getModel());
    }

    /**
     * 모델을 선택하여 단일 사용자 메시지를 간단하게 전송하는 헬퍼 메서드
     */
    public String sendSimpleMessage(String userMessage, String model) {
        OpenAiDto.Message message = OpenAiDto.Message.builder()
                .role("user")
                .content(userMessage)
                .build();

        OpenAiDto.ChatResponse response = sendMessage(List.of(message), model);

        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        }

        throw new RuntimeException("OpenAI로부터 응답이 없습니다");
    }

    /**
     * 시스템 프롬프트와 함께 메시지 전송
     * @param systemPrompt AI에 대한 시스템 지시사항
     * @param userMessage 사용자 메시지
     */
    public String sendMessageWithSystemPrompt(String systemPrompt, String userMessage) {
        return sendMessageWithSystemPrompt(systemPrompt, userMessage, properties.getModel());
    }

    /**
     * 시스템 프롬프트와 모델 선택으로 메시지 전송
     * @param systemPrompt AI에 대한 시스템 지시사항
     * @param userMessage 사용자 메시지
     * @param model 사용할 OpenAI 모델
     */
    public String sendMessageWithSystemPrompt(String systemPrompt, String userMessage, String model) {
        List<OpenAiDto.Message> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(OpenAiDto.Message.builder()
                    .role("system")
                    .content(systemPrompt)
                    .build());
        }

        messages.add(OpenAiDto.Message.builder()
                .role("user")
                .content(userMessage)
                .build());

        OpenAiDto.ChatResponse response = sendMessage(messages, model);

        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        }

        throw new RuntimeException("OpenAI로부터 응답이 없습니다");
    }

    /**
     * 스트리밍 방식으로 메시지 전송
     * @param messages 채팅 메시지 리스트
     * @param onChunk 각 스트리밍 청크를 처리할 콜백 함수
     */
    public void sendMessageStream(List<OpenAiDto.Message> messages, Consumer<String> onChunk) {
        sendMessageStream(messages, properties.getModel(), onChunk);
    }

    /**
     * 모델을 선택하여 스트리밍 방식으로 메시지 전송
     * @param messages 채팅 메시지 리스트
     * @param model 사용할 OpenAI 모델
     * @param onChunk 각 스트리밍 청크를 처리할 콜백 함수
     */
    public void sendMessageStream(List<OpenAiDto.Message> messages, String model, Consumer<String> onChunk) {
        sendMessageStream(messages, model, properties.getTemperature(), properties.getMaxTokens(), onChunk);
    }

    /**
     * 모든 옵션을 커스터마이징하여 스트리밍 방식으로 메시지 전송
     * @param messages 채팅 메시지 리스트
     * @param model 사용할 OpenAI 모델
     * @param temperature 랜덤성 조절 (0.0 ~ 2.0)
     * @param maxTokens 응답의 최대 토큰 수
     * @param onChunk 각 스트리밍 청크를 처리할 콜백 함수
     */
    public void sendMessageStream(
            List<OpenAiDto.Message> messages,
            String model,
            Double temperature,
            Integer maxTokens,
            Consumer<String> onChunk) {

        try {
            // 스트리밍이 활성화된 요청 본문 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model != null ? model : properties.getModel());
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature != null ? temperature : properties.getTemperature());
            requestBody.put("max_tokens", maxTokens != null ? maxTokens : properties.getMaxTokens());
            requestBody.put("stream", true);

            log.info("OpenAI 스트리밍 요청 전송 중, 모델: {}", model);

            // 요청 전송 및 응답 스트리밍
            Flux<String> responseFlux = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class);

            responseFlux.subscribe(
                    chunk -> {
                        try {
                            // SSE 형식 파싱: "data: {...}"
                            if (chunk.startsWith("data: ")) {
                                String jsonData = chunk.substring(6).trim();

                                // [DONE] 신호 건너뛰기
                                if ("[DONE]".equals(jsonData)) {
                                    log.info("OpenAI 스트리밍 완료");
                                    return;
                                }

                                // JSON 청크 파싱
                                ObjectMapper objectMapper = new ObjectMapper();
                                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                                objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

                                OpenAiDto.StreamResponse streamResponse = objectMapper.readValue(jsonData, OpenAiDto.StreamResponse.class);

                                // delta에서 content 추출
                                if (streamResponse.getChoices() != null && !streamResponse.getChoices().isEmpty()) {
                                    OpenAiDto.StreamChoice choice = streamResponse.getChoices().get(0);
                                    if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                                        onChunk.accept(choice.getDelta().getContent());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("스트리밍 청크 파싱 오류: {}", e.getMessage());
                        }
                    },
                    error -> log.error("스트리밍 오류: {}", error.getMessage(), error),
                    () -> log.debug("스트리밍 완료")
            );

        } catch (Exception e) {
            log.error("OpenAI 스트리밍 API 호출 오류: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI 스트리밍 API 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 시스템 프롬프트와 함께 스트리밍 방식으로 간단한 메시지 전송
     * @param systemPrompt AI에 대한 시스템 지시사항
     * @param userMessage 사용자 메시지
     * @param onChunk 각 스트리밍 청크를 처리할 콜백 함수
     */
    public void sendStreamWithSystemPrompt(String systemPrompt, String userMessage, Consumer<String> onChunk) {
        sendStreamWithSystemPrompt(systemPrompt, userMessage, properties.getModel(), onChunk);
    }

    /**
     * 시스템 프롬프트와 모델 선택으로 스트리밍 방식 메시지 전송
     * @param systemPrompt AI에 대한 시스템 지시사항
     * @param userMessage 사용자 메시지
     * @param model 사용할 OpenAI 모델
     * @param onChunk 각 스트리밍 청크를 처리할 콜백 함수
     */
    public void sendStreamWithSystemPrompt(String systemPrompt, String userMessage, String model, Consumer<String> onChunk) {
        List<OpenAiDto.Message> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(OpenAiDto.Message.builder()
                    .role("system")
                    .content(systemPrompt)
                    .build());
        }

        messages.add(OpenAiDto.Message.builder()
                .role("user")
                .content(userMessage)
                .build());

        sendMessageStream(messages, model, onChunk);
    }

    /**
     * 사용 가능한 모델 목록
     */
    /**
     * List of available OpenAI model names.
     * NOTE: This list should be periodically updated according to https://platform.openai.com/docs/models
     */
    public static class AvailableModels {
        public static final String GPT_3_5_TURBO = "gpt-3.5-turbo";
        public static final String GPT_4 = "gpt-4";
        public static final String GPT_4_TURBO = "gpt-4-turbo";
        public static final String GPT_4O = "gpt-4o";
    }
}
