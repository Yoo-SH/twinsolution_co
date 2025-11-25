package com.twinsolution.construction.api.openai;

import com.twinsolution.construction.dto.OpenAiDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/test/openai")
@RequiredArgsConstructor
public class OpenAiTestController {

    private final OpenAiChatClient openAiChatClient;

    /**
     * 기본 채팅 테스트
     */
    @GetMapping("/chat")
    public String testChat(@RequestParam(defaultValue = "Hello, how are you?") String message) {
        log.info("Testing basic chat with message: {}", message);
        try {
            String response = openAiChatClient.sendSimpleMessage(message);
            log.info("Chat response received: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Error in chat test: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 시스템 프롬프트와 함께 채팅 테스트
     */
    @GetMapping("/chat-with-system")
    public String testChatWithSystem(
            @RequestParam(defaultValue = "You are a helpful construction assistant") String systemPrompt,
            @RequestParam(defaultValue = "What can you help me with?") String message) {
        log.info("Testing chat with system prompt");
        try {
            String response = openAiChatClient.sendMessageWithSystemPrompt(systemPrompt, message);
            log.info("Chat response with system prompt received");
            return response;
        } catch (Exception e) {
            log.error("Error in chat with system test: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 스트리밍 채팅 테스트
     */
    @GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> testChatStream(@RequestParam(defaultValue = "Tell me a short story") String message) {
        log.info("Testing streaming chat with message: {}", message);

        return Flux.create(sink -> {
            try {
                OpenAiDto.Message userMessage = OpenAiDto.Message.builder()
                        .role("user")
                        .content(message)
                        .build();

                openAiChatClient.sendMessageStream(
                        List.of(userMessage),
                        chunk -> {
                            log.debug("Stream chunk received: {}", chunk);
                            sink.next(chunk);
                        }
                );

                // 스트리밍이 완료될 때까지 대기
                Thread.sleep(5000);
                sink.complete();
            } catch (Exception e) {
                log.error("Error in streaming test: {}", e.getMessage(), e);
                sink.error(e);
            }
        });
    }

    /**
     * 다양한 모델 테스트
     */
    @GetMapping("/chat-model")
    public String testChatWithModel(
            @RequestParam String model,
            @RequestParam(defaultValue = "Hello!") String message) {
        log.info("Testing chat with model: {}", model);
        try {
            String response = openAiChatClient.sendSimpleMessage(message, model);
            log.info("Chat response from model {} received", model);
            return response;
        } catch (Exception e) {
            log.error("Error in chat with model test: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 전체 응답 정보 테스트 (토큰 사용량 등)
     */
    @GetMapping("/chat-full")
    public OpenAiDto.ChatResponse testChatFull(@RequestParam(defaultValue = "Hello!") String message) {
        log.info("Testing full chat response");
        try {
            OpenAiDto.Message userMessage = OpenAiDto.Message.builder()
                    .role("user")
                    .content(message)
                    .build();

            OpenAiDto.ChatResponse response = openAiChatClient.sendMessage(List.of(userMessage));
            log.info("Full chat response received with {} tokens used",
                    response.getUsage() != null ? response.getUsage().getTotalTokens() : "unknown");
            return response;
        } catch (Exception e) {
            log.error("Error in full chat test: {}", e.getMessage(), e);
            throw new RuntimeException("Error: " + e.getMessage(), e);
        }
    }

    /**
     * 건설 관련 질문 테스트
     */
    @GetMapping("/construction-qa")
    public String testConstructionQA(@RequestParam String question) {
        log.info("Testing construction Q&A with question: {}", question);
        try {
            String systemPrompt = "You are an expert construction consultant. " +
                    "Provide detailed, accurate, and practical answers about construction, " +
                    "building materials, safety regulations, and project management.";

            String response = openAiChatClient.sendMessageWithSystemPrompt(systemPrompt, question);
            log.info("Construction Q&A response received");
            return response;
        } catch (Exception e) {
            log.error("Error in construction Q&A test: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }
}
