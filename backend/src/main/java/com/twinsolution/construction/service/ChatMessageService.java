package com.twinsolution.construction.service;

import com.twinsolution.construction.api.openai.OpenAiChatClient;
import com.twinsolution.construction.config.LangChainConfig;
import com.twinsolution.construction.dto.ChatMessageDto;
import com.twinsolution.construction.dto.OpenAiDto;
import com.twinsolution.construction.entity.ChatMessage;
import com.twinsolution.construction.entity.ChatSession;
import com.twinsolution.construction.exception.ResourceNotFoundException;
import com.twinsolution.construction.repository.ChatMessageRepository;
import com.twinsolution.construction.repository.ChatSessionRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final OpenAiChatClient openAiChatClient;

    // LangChain4j 컴포넌트
    private final ChatLanguageModel chatLanguageModel;
    private final StreamingChatLanguageModel streamingChatLanguageModel;
    private final Map<Long, ChatMemory> chatMemoryStore;
    private final LangChainConfig langChainConfig;

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

        // 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(session)
                .role("user")
                .content(request.getContent())
                .build();
        chatMessageRepository.save(userMessage);

        // LangChain4j를 사용한 멀티턴 대화 생성
        String aiResponse = generateMultiTurnAIResponse(sessionId, request);

        // AI 응답 메시지 저장
        ChatMessage assistantMessage = ChatMessage.builder()
                .chatSession(session)
                .role("assistant")
                .content(aiResponse)
                .build();
        ChatMessage savedMessage = chatMessageRepository.save(assistantMessage);

        return convertToResponse(savedMessage);
    }

    /**
     * LangChain4j를 사용한 멀티턴 대화 생성 (일반 응답)
     */
    private String generateMultiTurnAIResponse(Long sessionId, ChatMessageDto.Request request) {
        // 세션별 ChatMemory 가져오거나 생성
        ChatMemory chatMemory = chatMemoryStore.computeIfAbsent(sessionId,
            id -> {
                ChatMemory memory = langChainConfig.createChatMemory(20); // 최근 20개 메시지 유지

                // 시스템 프롬프트가 있으면 추가
                if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
                    memory.add(SystemMessage.from(request.getSystemPrompt()));
                }

                // DB에서 기존 대화 이력 로드
                loadHistoryIntoMemory(sessionId, memory);

                return memory;
            });

        // 현재 사용자 메시지를 메모리에 추가
        chatMemory.add(UserMessage.from(request.getContent()));

        // LangChain4j로 AI 응답 생성
        Response<AiMessage> response = chatLanguageModel.generate(chatMemory.messages());
        String aiResponseText = response.content().text();

        // AI 응답을 메모리에 추가
        chatMemory.add(response.content());

        log.info("멀티턴 대화 생성 완료. 세션 ID: {}, 메모리 크기: {}", sessionId, chatMemory.messages().size());

        return aiResponseText;
    }

    /**
     * DB에서 기존 대화 이력을 ChatMemory에 로드
     */
    private void loadHistoryIntoMemory(Long sessionId, ChatMemory chatMemory) {
        List<ChatMessage> history = chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(sessionId);

        for (ChatMessage msg : history) {
            if ("user".equals(msg.getRole())) {
                chatMemory.add(UserMessage.from(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                chatMemory.add(AiMessage.from(msg.getContent()));
            }
        }

        log.debug("세션 {}에 {} 개의 메시지 이력 로드 완료", sessionId, history.size());
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
     * SseEmitter를 사용한 멀티턴 스트리밍 방식으로 메시지 전송
     * @param sessionId 세션 ID
     * @param request 채팅 메시지 요청
     * @return SseEmitter
     */
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

        // 전체 응답을 모으기 위한 StringBuilder
        StringBuilder fullResponse = new StringBuilder();

        // 세션별 ChatMemory 가져오거나 생성
        ChatMemory chatMemory = chatMemoryStore.computeIfAbsent(sessionId,
            id -> {
                ChatMemory memory = langChainConfig.createChatMemory(20);

                // 시스템 프롬프트가 있으면 추가
                if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
                    memory.add(SystemMessage.from(request.getSystemPrompt()));
                }

                // DB에서 기존 대화 이력 로드
                loadHistoryIntoMemory(sessionId, memory);

                return memory;
            });

        // 현재 사용자 메시지를 메모리에 추가
        chatMemory.add(UserMessage.from(request.getContent()));

        // 비동기 실행
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicReference<String> aiResponseRef = new AtomicReference<>("");

        executor.submit(() -> {
            try {
                log.info("LangChain4j 멀티턴 스트리밍 시작. 세션 ID: {}", sessionId);

                // LangChain4j 스트리밍 모델 사용
                streamingChatLanguageModel.generate(
                    chatMemory.messages(),
                    new dev.langchain4j.model.StreamingResponseHandler<AiMessage>() {
                        @Override
                        public void onNext(String token) {
                            try {
                                fullResponse.append(token);
                                emitter.send(SseEmitter.event()
                                        .data(token)
                                        .name("message"));
                                log.debug("청크 전송: {}", token);
                            } catch (IOException e) {
                                log.error("SSE 전송 오류: {}", e.getMessage());
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void onComplete(Response<AiMessage> response) {
                            try {
                                String completeResponse = fullResponse.toString();
                                aiResponseRef.set(completeResponse);

                                // AI 응답을 메모리에 추가
                                chatMemory.add(response.content());

                                // 스트리밍 완료 후 AI 응답 메시지 저장
                                ChatMessage assistantMessage = ChatMessage.builder()
                                        .chatSession(session)
                                        .role("assistant")
                                        .content(completeResponse)
                                        .build();

                                chatMessageRepository.save(assistantMessage);
                                log.info("AI 응답 저장 완료. 세션 ID: {}, 응답 길이: {}, 메모리 크기: {}",
                                        sessionId, completeResponse.length(), chatMemory.messages().size());

                                // 완료 이벤트 전송
                                emitter.send(SseEmitter.event()
                                        .data("[DONE]")
                                        .name("done"));
                                emitter.complete();
                            } catch (IOException e) {
                                log.error("완료 처리 오류: {}", e.getMessage());
                                emitter.completeWithError(e);
                            } finally {
                                executor.shutdown();
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            log.error("스트리밍 중 오류 발생: {}", error.getMessage(), error);
                            emitter.completeWithError(error);
                            executor.shutdown();
                        }
                    }
                );

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
