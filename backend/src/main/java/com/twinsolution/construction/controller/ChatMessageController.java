package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.ChatMessageDto;
import com.twinsolution.construction.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat-sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageDto.Response>> getMessages(
            @PathVariable Long sessionId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        List<ChatMessageDto.Response> messages;
        if (page != null && size != null) {
            messages = chatMessageService.getMessagesBySessionId(sessionId, page, size);
        } else {
            messages = chatMessageService.getMessagesBySessionId(sessionId);
        }
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{sessionId}/messages")
    public ResponseEntity<ChatMessageDto.Response> sendMessage(
            @PathVariable Long sessionId,
            @RequestBody ChatMessageDto.Request request) {
        ChatMessageDto.Response response = chatMessageService.sendMessage(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
