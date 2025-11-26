package com.twinsolution.construction.service;

import com.twinsolution.construction.dto.ChatSessionDto;
import com.twinsolution.construction.dto.DocumentDto;
import com.twinsolution.construction.entity.ChatSession;
import com.twinsolution.construction.entity.Project;
import com.twinsolution.construction.exception.ResourceNotFoundException;
import com.twinsolution.construction.repository.ChatSessionRepository;
import com.twinsolution.construction.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ProjectRepository projectRepository;
    private final DocumentService documentService;

    @Transactional
    public ChatSessionDto.Response createChatSession(ChatSessionDto.Request request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("프로젝트", "ID", request.getProjectId()));

        String defaultQuickQuestions = request.getQuickQuestion() != null
                ? request.getQuickQuestion()
                : "[\"준주거지역에 필요한 서류는?\", \"내진설계 의무 대상\", \"주차장 설치 기준\", \"건축허가신청서 작성 방법\"]";

        ChatSession chatSession = ChatSession.builder()
                .project(project)
                .quickQuestion(defaultQuickQuestions)
                .build();

        ChatSession savedSession = chatSessionRepository.save(chatSession);
        return convertToResponse(savedSession);
    }

    public List<ChatSessionDto.Response> getChatSessionsByProjectId(Long projectId) {
        List<ChatSession> sessions = chatSessionRepository.findByProjectIdOrderByUpdatedAtDesc(projectId);
        return sessions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ChatSessionDto.Response getChatSessionById(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("채팅 세션", "ID", sessionId));
        return convertToResponse(session);
    }

    @Transactional
    public ChatSessionDto.Response updateQuickQuestions(Long sessionId, ChatSessionDto.QuickQuestionRequest request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("채팅 세션", "ID", sessionId));

        session.setQuickQuestion(request.getQuickQuestion());
        return convertToResponse(session);
    }

    @Transactional
    public List<DocumentDto.Response> createGeneratedDocuments(Long sessionId, ChatSessionDto.GeneratedDocumentRequest request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("채팅 세션", "ID", sessionId));

        return request.getDocuments().stream()
                .map(docItem -> {
                    DocumentDto.Request docRequest = DocumentDto.Request.builder()
                            .projectId(session.getProject().getId())
                            .name(docItem.getName())
                            .fileType(docItem.getFileType())
                            .filePath("generated/" + docItem.getName())
                            .origin("generated")
                            .status("템플릿 생성")
                            .build();
                    return documentService.createGeneratedDocument(docRequest);
                })
                .collect(Collectors.toList());
    }

    public long countSessionsCreatedAfter(LocalDateTime date) {
        return chatSessionRepository.countSessionsCreatedAfter(date);
    }

    private ChatSessionDto.Response convertToResponse(ChatSession session) {
        return ChatSessionDto.Response.builder()
                .id(session.getId())
                .projectId(session.getProject().getId())
                .quickQuestion(session.getQuickQuestion())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
