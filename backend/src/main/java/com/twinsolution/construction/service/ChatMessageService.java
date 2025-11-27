package com.twinsolution.construction.service;

import com.twinsolution.construction.api.openai.OpenAiChatClient;
import com.twinsolution.construction.dto.ChatMessageDto;
import com.twinsolution.construction.dto.OpenAiDto;
import com.twinsolution.construction.entity.ChatMessage;
import com.twinsolution.construction.entity.ChatSession;
import com.twinsolution.construction.exception.ResourceNotFoundException;
import com.twinsolution.construction.repository.ChatMessageRepository;
import com.twinsolution.construction.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final OpenAiChatClient openAiChatClient;

    public List<ChatMessageDto.Response> getMessagesBySessionId(Long sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(sessionId);
        return messages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ChatMessageDto.Response> getMessagesBySessionId(Long sessionId, int page, int size) {
        List<ChatMessage> messages = chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(
                sessionId, PageRequest.of(page, size));
        return messages.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageDto.Response sendMessage(Long sessionId, ChatMessageDto.Request request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("채팅 세션", "ID", sessionId));

        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(session)
                .role("user")
                .content(request.getContent())
                .build();

        chatMessageRepository.save(userMessage);

        String aiResponse = generateAIResponse(request, session);

        ChatMessage assistantMessage = ChatMessage.builder()
                .chatSession(session)
                .role("assistant")
                .content(aiResponse)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(assistantMessage);

        return convertToResponse(savedMessage);
    }

    private String generateAIResponse(ChatMessageDto.Request request, ChatSession session) {
        // TODO: Implement RAG-based context retrieval
        // 1. Retrieve relevant document chunks from the project
        // 2. Add context to system prompt

        List<OpenAiDto.Message> messages = new ArrayList<>();

        // 시스템 프롬프트 추가 (있는 경우)
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            messages.add(OpenAiDto.Message.builder()
                    .role("system")
                    .content(request.getSystemPrompt())
                    .build());
        }

        // 사용자 메시지 추가
        messages.add(OpenAiDto.Message.builder()
                .role("user")
                .content(request.getContent())
                .build());

        // OpenAI API 호출 (기본값 설정)
        String model = request.getModel() != null ? request.getModel() : "gpt-3.5-turbo";
        Double temperature = request.getTemperature() != null ? request.getTemperature() : 0.7;
        Integer maxTokens = request.getMaxTokens() != null ? request.getMaxTokens() : 1000;

        OpenAiDto.ChatResponse response = openAiChatClient.sendMessage(
                messages,
                model,
                temperature,
                maxTokens
        );

        // 응답 추출
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        }

        throw new RuntimeException("OpenAI로부터 응답을 받지 못했습니다");
    }

    /**
     * SseEmitter를 사용한 스트리밍 방식으로 메시지 전송
     * @param sessionId 세션 ID
     * @param request 채팅 메시지 요청
     * @return SseEmitter
     */
    @Transactional
    public SseEmitter sendMessageStreamWithEmitter(Long sessionId, ChatMessageDto.Request request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("채팅 세션", "ID", sessionId));

        // 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(session)
                .role("user")
                .content(request.getContent())
                .build();

        chatMessageRepository.save(userMessage);

        // SseEmitter 생성 (타임아웃: 5분)
        SseEmitter emitter = new SseEmitter(300000L);

        // 스트리밍 응답을 위한 준비
        List<OpenAiDto.Message> messages = new ArrayList<>();

        // 시스템 프롬프트 추가 (있는 경우)
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            messages.add(OpenAiDto.Message.builder()
                    .role("system")
                    .content(request.getSystemPrompt())
                    .build());
        }

        // 사용자 메시지 추가
        messages.add(OpenAiDto.Message.builder()
                .role("user")
                .content(request.getContent())
                .build());

        // OpenAI API 호출 설정 (기본값 설정)
        String model = request.getModel() != null ? request.getModel() : "gpt-3.5-turbo";
        Double temperature = request.getTemperature() != null ? request.getTemperature() : 0.7;
        Integer maxTokens = request.getMaxTokens() != null ? request.getMaxTokens() : 1000;

        // 전체 응답을 모으기 위한 StringBuilder
        StringBuilder fullResponse = new StringBuilder();

        // 비동기 실행
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                log.info("OpenAI 스트리밍 시작. 세션 ID: {}", sessionId);

                // Flux 기반 스트리밍으로 변경
                openAiChatClient.sendMessageStreamFlux(messages, model, temperature, maxTokens)
                        .doOnNext(chunk -> {
                            try {
                                fullResponse.append(chunk);
                                emitter.send(SseEmitter.event()
                                        .data(chunk)
                                        .name("message"));
                                log.debug("청크 전송: {}", chunk);
                            } catch (IOException e) {
                                log.error("SSE 전송 오류: {}", e.getMessage());
                                throw new RuntimeException(e);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                // 스트리밍 완료 후 AI 응답 메시지 저장
                                ChatMessage assistantMessage = ChatMessage.builder()
                                        .chatSession(session)
                                        .role("assistant")
                                        .content(fullResponse.toString())
                                        .build();

                                chatMessageRepository.save(assistantMessage);
                                log.info("AI 응답 저장 완료. 세션 ID: {}, 응답 길이: {}", sessionId, fullResponse.length());

                                // 완료 이벤트 전송
                                emitter.send(SseEmitter.event()
                                        .data("[DONE]")
                                        .name("done"));
                                emitter.complete();
                            } catch (IOException e) {
                                log.error("완료 처리 오류: {}", e.getMessage());
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnError(e -> {
                            log.error("스트리밍 중 오류 발생: {}", e.getMessage(), e);
                            emitter.completeWithError(e);
                        })
                        .doFinally(signal -> executor.shutdown())
                        .blockLast(); // 스트리밍이 완전히 끝날 때까지 대기

            } catch (Exception e) {
                log.error("스트리밍 중 오류 발생: {}", e.getMessage(), e);
                emitter.completeWithError(e);
                executor.shutdown();
            }
        });

        // 타임아웃 및 완료 핸들러
        emitter.onTimeout(() -> {
            log.warn("SSE 타임아웃. 세션 ID: {}", sessionId);
            emitter.complete();
            executor.shutdown();
        });

        emitter.onCompletion(() -> {
            log.info("SSE 완료. 세션 ID: {}", sessionId);
            executor.shutdown();
        });

        emitter.onError((e) -> {
            log.error("SSE 오류. 세션 ID: {}, 오류: {}", sessionId, e.getMessage());
            executor.shutdown();
        });

        return emitter;
    }

    /**
     * Flux를 사용한 스트리밍 방식으로 메시지 전송 (Reactive 환경용)
     * @param sessionId 세션 ID
     * @param request 채팅 메시지 요청
     * @return 스트리밍 응답 Flux
     */
    @Transactional
    public Flux<String> sendMessageStream(Long sessionId, ChatMessageDto.Request request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("채팅 세션", "ID", sessionId));

        // 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(session)
                .role("user")
                .content(request.getContent())
                .build();

        chatMessageRepository.save(userMessage);

        // 스트리밍 응답을 위한 준비
        List<OpenAiDto.Message> messages = new ArrayList<>();

        // 시스템 프롬프트 추가 (있는 경우)
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            messages.add(OpenAiDto.Message.builder()
                    .role("system")
                    .content(request.getSystemPrompt())
                    .build());
        }

        // 사용자 메시지 추가
        messages.add(OpenAiDto.Message.builder()
                .role("user")
                .content(request.getContent())
                .build());

        // OpenAI API 호출 설정 (기본값 설정)
        String model = request.getModel() != null ? request.getModel() : "gpt-3.5-turbo";
        Double temperature = request.getTemperature() != null ? request.getTemperature() : 0.7;
        Integer maxTokens = request.getMaxTokens() != null ? request.getMaxTokens() : 1000;

        // 전체 응답을 모으기 위한 StringBuilder
        StringBuilder fullResponse = new StringBuilder();

        // 스트리밍 응답 생성 및 반환
        return openAiChatClient.sendMessageStreamFlux(messages, model, temperature, maxTokens)
                .doOnNext(chunk -> {
                    // 각 청크를 전체 응답에 추가
                    fullResponse.append(chunk);
                })
                .doOnComplete(() -> {
                    // 스트리밍 완료 후 AI 응답 메시지 저장
                    ChatMessage assistantMessage = ChatMessage.builder()
                            .chatSession(session)
                            .role("assistant")
                            .content(fullResponse.toString())
                            .build();

                    chatMessageRepository.save(assistantMessage);
                    log.info("AI 응답 저장 완료. 세션 ID: {}, 응답 길이: {}", sessionId, fullResponse.length());
                })
                .doOnError(e -> log.error("스트리밍 중 오류 발생: {}", e.getMessage(), e));
    }

    private ChatMessageDto.Response convertToResponse(ChatMessage message) {
        return ChatMessageDto.Response.builder()
                .id(message.getId())
                .sessionId(message.getChatSession().getId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
