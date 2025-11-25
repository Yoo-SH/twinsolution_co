package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.ChatSessionDto;
import com.twinsolution.construction.dto.DocumentDto;
import com.twinsolution.construction.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    @PostMapping("/projects/{projectId}/chat-sessions")
    public ResponseEntity<ChatSessionDto.Response> createChatSession(
            @PathVariable Long projectId,
            @RequestBody(required = false) ChatSessionDto.Request request) {
        if (request == null) {
            request = new ChatSessionDto.Request();
            request.setProjectId(projectId);
        } else {
            request.setProjectId(projectId);
        }
        ChatSessionDto.Response response = chatSessionService.createChatSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projects/{projectId}/chat-sessions")
    public ResponseEntity<List<ChatSessionDto.Response>> getChatSessionsByProject(@PathVariable Long projectId) {
        List<ChatSessionDto.Response> sessions = chatSessionService.getChatSessionsByProjectId(projectId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/chat-sessions/{sessionId}")
    public ResponseEntity<ChatSessionDto.Response> getChatSession(@PathVariable Long sessionId) {
        ChatSessionDto.Response session = chatSessionService.getChatSessionById(sessionId);
        return ResponseEntity.ok(session);
    }

    @PutMapping("/chat-sessions/{sessionId}/quick-questions")
    public ResponseEntity<ChatSessionDto.Response> updateQuickQuestions(
            @PathVariable Long sessionId,
            @RequestBody ChatSessionDto.QuickQuestionRequest request) {
        ChatSessionDto.Response response = chatSessionService.updateQuickQuestions(sessionId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat-sessions/{sessionId}/generated-documents")
    public ResponseEntity<List<DocumentDto.Response>> createGeneratedDocuments(
            @PathVariable Long sessionId,
            @RequestBody ChatSessionDto.GeneratedDocumentRequest request) {
        List<DocumentDto.Response> documents = chatSessionService.createGeneratedDocuments(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(documents);
    }
}
