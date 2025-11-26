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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        // OpenAI API 호출
        OpenAiDto.ChatResponse response = openAiChatClient.sendMessage(
                messages,
                request.getModel(),
                request.getTemperature(),
                request.getMaxTokens()
        );

        // 응답 추출
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getMessage().getContent();
        }

        throw new RuntimeException("OpenAI로부터 응답을 받지 못했습니다");
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
