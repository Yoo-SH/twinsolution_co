package com.twinsolution.construction.service;

import com.twinsolution.construction.api.openai.OpenAiChatClient;
import com.twinsolution.construction.config.LangChainConfig;
import com.twinsolution.construction.dto.ChatMessageDto;
import com.twinsolution.construction.dto.OpenAiDto;
import com.twinsolution.construction.dto.RagDto;
import com.twinsolution.construction.entity.ChatMessage;
import com.twinsolution.construction.entity.ChatSession;
import com.twinsolution.construction.entity.Project;
import com.twinsolution.construction.exception.ResourceNotFoundException;
import com.twinsolution.construction.llm.LLMProviderFactory;
import com.twinsolution.construction.repository.ChatMessageRepository;
import com.twinsolution.construction.repository.ChatSessionRepository;
import org.springframework.web.reactive.function.client.WebClient;
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
    private final WebClient ragWebClient;  // RAG 서비스 클라이언트

    // LangChain4j 컴포넌트
    private final LLMProviderFactory llmProviderFactory;  // 프로젝트별 LLM 제공
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
     * RAG 기반 컨텍스트 검색 포함
     * 프로젝트 설정에 따라 OpenAI 또는 Ollama 모델 사용
     */
    private String generateMultiTurnAIResponse(Long sessionId, ChatMessageDto.Request request) {
        // 세션 정보 가져오기
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("채팅 세션", "ID", sessionId));

        Project project = session.getProject();

        // RAG 기반 컨텍스트 검색 (프로젝트별 문서 검색)
        String ragContext = retrieveRagContext(project.getId(), request.getContent());

        // 세션별 ChatMemory 가져오거나 생성
        ChatMemory chatMemory = chatMemoryStore.computeIfAbsent(sessionId,
            id -> {
                ChatMemory memory = langChainConfig.createChatMemory(20); // 최근 20개 메시지 유지

                // 시스템 프롬프트 생성 (RAG 컨텍스트 포함)
                String systemPrompt = buildSystemPromptWithRagContext(
                    request.getSystemPrompt(),
                    ragContext
                );

                if (systemPrompt != null && !systemPrompt.isEmpty()) {
                    memory.add(SystemMessage.from(systemPrompt));
                }

                // DB에서 기존 대화 이력 로드
                loadHistoryIntoMemory(sessionId, memory);

                return memory;
            });

        // 현재 사용자 메시지를 메모리에 추가
        chatMemory.add(UserMessage.from(request.getContent()));

        // 요청의 LLM 설정 사용 (없으면 기본값 OPENAI/gpt-3.5-turbo)
        String llmProvider = request.getLlmProvider() != null ? request.getLlmProvider() : "OPENAI";
        String modelName = request.getModel() != null ? request.getModel() : "gpt-3.5-turbo";

        // LLM 모델 가져오기
        ChatLanguageModel chatModel = llmProviderFactory.getChatModel(llmProvider, modelName);

        log.info("LLM 모델 사용: Provider={}, Model={}, Session ID={}",
                llmProvider, modelName, sessionId);

        // LangChain4j로 AI 응답 생성
        Response<AiMessage> response = chatModel.generate(chatMemory.messages());
        String aiResponseText = response.content().text();

        // AI 응답을 메모리에 추가
        chatMemory.add(response.content());

        log.info("멀티턴 대화 생성 완료. 세션 ID: {}, 메모리 크기: {}, RAG 컨텍스트: {}자",
                sessionId, chatMemory.messages().size(), ragContext != null ? ragContext.length() : 0);

        return aiResponseText;
    }

    /**
     * RAG 서비스에서 관련 문서 검색 (프로젝트별 격리)
     */
    private String retrieveRagContext(Long projectId, String query) {
        try {
            RagDto.SearchRequest ragRequest = RagDto.SearchRequest.builder()
                    .query(query)
                    .projectId(projectId.toString())  // 프로젝트 ID로 필터링
                    .topK(5)
                    .similarityThreshold(0.3f)
                    .enableReranking(false)
                    .maxContextLength(4000)
                    .build();

            RagDto.SearchResponse ragResponse = ragWebClient.post()
                    .uri("/api/v1/RAG/search")
                    .bodyValue(ragRequest)
                    .retrieve()
                    .bodyToMono(RagDto.SearchResponse.class)
                    .block();

            if (ragResponse != null && ragResponse.getSuccess() && ragResponse.getContext() != null) {
                log.info("RAG 검색 성공. 프로젝트 ID: {}, 결과 개수: {}, 컨텍스트 길이: {}",
                        projectId, ragResponse.getTotalResults(), ragResponse.getContext().length());
                return ragResponse.getContext();
            }

            log.debug("RAG 검색 결과 없음. 프로젝트 ID: {}", projectId);
            return null;

        } catch (Exception e) {
            log.error("RAG 검색 오류. 프로젝트 ID: {}, 오류: {}", projectId, e.getMessage());
            return null;
        }
    }

    /**
     * RAG 컨텍스트를 포함한 시스템 프롬프트 생성
     */
    private String buildSystemPromptWithRagContext(String originalSystemPrompt, String ragContext) {
        StringBuilder systemPrompt = new StringBuilder();

        // 기본 시스템 프롬프트
        if (originalSystemPrompt != null && !originalSystemPrompt.isEmpty()) {
            systemPrompt.append(originalSystemPrompt);
        } else {
            systemPrompt.append("당신은 건축 프로젝트를 지원하는 AI 어시스턴트입니다.");
        }

        // RAG 컨텍스트 추가
        if (ragContext != null && !ragContext.isEmpty()) {
            systemPrompt.append("\n\n");
            systemPrompt.append("다음은 프로젝트 문서에서 검색한 관련 정보입니다:\n\n");
            systemPrompt.append(ragContext);
            systemPrompt.append("\n\n위 정보를 참고하여 사용자의 질문에 답변해주세요.");
        }

        return systemPrompt.toString();
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
     * RAG 기반 컨텍스트 검색 포함
     * 프로젝트 설정에 따라 OpenAI 또는 Ollama 모델 사용
     * @param sessionId 세션 ID
     * @param request 채팅 메시지 요청
     * @return SseEmitter
     */
    public SseEmitter sendMessageStreamWithEmitter(Long sessionId, ChatMessageDto.Request request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("채팅 세션", "ID", sessionId));

        Project project = session.getProject();

        // 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(session)
                .role("user")
                .content(request.getContent())
                .build();
        chatMessageRepository.save(userMessage);

        // RAG 기반 컨텍스트 검색 (프로젝트별 문서 검색)
        String ragContext = retrieveRagContext(project.getId(), request.getContent());

        // SseEmitter 생성 (타임아웃: 5분)
        SseEmitter emitter = new SseEmitter(300000L);

        // 전체 응답을 모으기 위한 StringBuilder
        StringBuilder fullResponse = new StringBuilder();

        // 세션별 ChatMemory 가져오거나 생성
        ChatMemory chatMemory = chatMemoryStore.computeIfAbsent(sessionId,
            id -> {
                ChatMemory memory = langChainConfig.createChatMemory(20);

                // 시스템 프롬프트 생성 (RAG 컨텍스트 포함)
                String systemPrompt = buildSystemPromptWithRagContext(
                    request.getSystemPrompt(),
                    ragContext
                );

                if (systemPrompt != null && !systemPrompt.isEmpty()) {
                    memory.add(SystemMessage.from(systemPrompt));
                }

                // DB에서 기존 대화 이력 로드
                loadHistoryIntoMemory(sessionId, memory);

                return memory;
            });

        // 현재 사용자 메시지를 메모리에 추가
        chatMemory.add(UserMessage.from(request.getContent()));

        // 요청의 LLM 설정 사용 (없으면 기본값 OPENAI/gpt-3.5-turbo)
        String llmProvider = request.getLlmProvider() != null ? request.getLlmProvider() : "OPENAI";
        String modelName = request.getModel() != null ? request.getModel() : "gpt-3.5-turbo";

        // 스트리밍 LLM 모델 가져오기
        StreamingChatLanguageModel streamingModel = llmProviderFactory.getStreamingChatModel(llmProvider, modelName);

        log.info("스트리밍 LLM 모델 사용: Provider={}, Model={}, Session ID={}",
                llmProvider, modelName, sessionId);

        // 비동기 실행
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicReference<String> aiResponseRef = new AtomicReference<>("");

        executor.submit(() -> {
            try {
                log.info("LangChain4j 멀티턴 스트리밍 시작. 세션 ID: {}", sessionId);

                // LangChain4j 스트리밍 모델 사용 (프로젝트별 모델)
                streamingModel.generate(
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
