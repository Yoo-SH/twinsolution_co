package com.twinsolution.construction.service;

import com.twinsolution.construction.dto.ChatMessageDto;
import com.twinsolution.construction.entity.ChatMessage;
import com.twinsolution.construction.entity.ChatSession;
import com.twinsolution.construction.exception.ResourceNotFoundException;
import com.twinsolution.construction.repository.ChatMessageRepository;
import com.twinsolution.construction.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;

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

        String aiResponse = generateAIResponse(request.getContent(), session);

        ChatMessage assistantMessage = ChatMessage.builder()
                .chatSession(session)
                .role("assistant")
                .content(aiResponse)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(assistantMessage);

        return convertToResponse(savedMessage);
    }

    private String generateAIResponse(String userMessage, ChatSession session) {
        // TODO: Implement actual RAG-based AI response generation
        // This should:
        // 1. Retrieve relevant document chunks from the project
        // 2. Call external API if needed
        // 3. Generate response using LLM with context

        return "AI 응답: " + userMessage + "에 대한 답변입니다.";
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
