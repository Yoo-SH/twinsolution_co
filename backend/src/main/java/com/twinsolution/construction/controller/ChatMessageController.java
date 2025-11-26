package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.ChatMessageDto;
import com.twinsolution.construction.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "채팅 메시지", description = "AI 챗봇과의 대화 메시지를 관리하는 API입니다. 메시지 전송 및 대화 이력 조회 기능을 제공합니다.")
@RestController
@RequestMapping("/api/chat-sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @Operation(
            summary = "채팅 세션의 메시지 목록 조회",
            description = "특정 채팅 세션의 전체 대화 이력을 조회합니다. " +
                    "사용자와 AI의 모든 대화 내용을 시간순으로 확인할 수 있습니다. " +
                    "페이지네이션 파라미터를 사용하여 메시지를 나누어 조회할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "메시지 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ChatMessageDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 세션을 찾을 수 없습니다.")
    })
    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageDto.Response>> getMessages(
            @Parameter(description = "채팅 세션 ID", required = true, example = "1")
            @PathVariable Long sessionId,
            @Parameter(description = "페이지 번호 (0부터 시작, 선택사항)", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지당 메시지 수 (선택사항)", example = "20")
            @RequestParam(required = false) Integer size) {
        List<ChatMessageDto.Response> messages;
        if (page != null && size != null) {
            messages = chatMessageService.getMessagesBySessionId(sessionId, page, size);
        } else {
            messages = chatMessageService.getMessagesBySessionId(sessionId);
        }
        return ResponseEntity.ok(messages);
    }

    @Operation(
            summary = "메시지 전송 및 AI 응답 받기",
            description = "채팅 세션에 사용자 메시지를 전송하고 AI로부터 응답을 받습니다. " +
                    "전송된 메시지는 프로젝트 문서를 기반으로 AI가 분석하여 답변을 생성합니다. " +
                    "응답에는 사용자 메시지와 AI 응답이 모두 포함됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "메시지 전송 및 AI 응답 생성 성공",
                    content = @Content(schema = @Schema(implementation = ChatMessageDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 세션을 찾을 수 없습니다."),
            @ApiResponse(responseCode = "400", description = "잘못된 메시지 형식입니다."),
            @ApiResponse(responseCode = "500", description = "AI 응답 생성 중 오류가 발생했습니다.")
    })
    @PostMapping("/{sessionId}/messages")
    public ResponseEntity<ChatMessageDto.Response> sendMessage(
            @Parameter(description = "채팅 세션 ID", required = true, example = "1")
            @PathVariable Long sessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "전송할 메시지 내용",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ChatMessageDto.Request.class))
            )
            @RequestBody ChatMessageDto.Request request) {
        ChatMessageDto.Response response = chatMessageService.sendMessage(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
