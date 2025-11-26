package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.ChatSessionDto;
import com.twinsolution.construction.dto.DocumentDto;
import com.twinsolution.construction.service.ChatSessionService;
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

@Tag(name = "채팅 세션", description = "AI 챗봇 대화 세션을 관리하는 API입니다. 세션 생성, 조회, 빠른 질문 관리, 생성 문서 등록 기능을 제공합니다.")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    @Operation(
            summary = "새로운 채팅 세션 생성",
            description = "프로젝트에 새로운 AI 챗봇 대화 세션을 생성합니다. " +
                    "생성된 세션은 해당 프로젝트의 문서를 기반으로 AI와 대화할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "채팅 세션 생성 성공",
                    content = @Content(schema = @Schema(implementation = ChatSessionDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다.")
    })
    @PostMapping("/projects/{projectId}/chat-sessions")
    public ResponseEntity<ChatSessionDto.Response> createChatSession(
            @Parameter(description = "채팅 세션을 생성할 프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "세션 생성 정보 (선택사항)",
                    content = @Content(schema = @Schema(implementation = ChatSessionDto.Request.class))
            )
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

    @Operation(
            summary = "프로젝트의 채팅 세션 목록 조회",
            description = "특정 프로젝트에 속한 모든 채팅 세션을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "채팅 세션 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ChatSessionDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다.")
    })
    @GetMapping("/projects/{projectId}/chat-sessions")
    public ResponseEntity<List<ChatSessionDto.Response>> getChatSessionsByProject(
            @Parameter(description = "프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId) {
        List<ChatSessionDto.Response> sessions = chatSessionService.getChatSessionsByProjectId(projectId);
        return ResponseEntity.ok(sessions);
    }

    @Operation(
            summary = "채팅 세션 상세 조회",
            description = "특정 채팅 세션의 상세 정보를 조회합니다. 세션 정보, 대화 이력 요약 등을 확인할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "채팅 세션 조회 성공",
                    content = @Content(schema = @Schema(implementation = ChatSessionDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 세션을 찾을 수 없습니다.")
    })
    @GetMapping("/chat-sessions/{sessionId}")
    public ResponseEntity<ChatSessionDto.Response> getChatSession(
            @Parameter(description = "채팅 세션 ID", required = true, example = "1")
            @PathVariable Long sessionId) {
        ChatSessionDto.Response session = chatSessionService.getChatSessionById(sessionId);
        return ResponseEntity.ok(session);
    }

    @Operation(
            summary = "빠른 질문 업데이트",
            description = "채팅 세션의 빠른 질문(추천 질문) 목록을 업데이트합니다. " +
                    "사용자가 자주 물어보는 질문이나 추천 질문을 설정할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "빠른 질문 업데이트 성공",
                    content = @Content(schema = @Schema(implementation = ChatSessionDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 세션을 찾을 수 없습니다.")
    })
    @PutMapping("/chat-sessions/{sessionId}/quick-questions")
    public ResponseEntity<ChatSessionDto.Response> updateQuickQuestions(
            @Parameter(description = "채팅 세션 ID", required = true, example = "1")
            @PathVariable Long sessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "빠른 질문 목록",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ChatSessionDto.QuickQuestionRequest.class))
            )
            @RequestBody ChatSessionDto.QuickQuestionRequest request) {
        ChatSessionDto.Response response = chatSessionService.updateQuickQuestions(sessionId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "AI 생성 문서 등록",
            description = "채팅 세션에서 AI가 생성한 문서를 프로젝트에 등록합니다. " +
                    "대화 중 AI가 작성한 보고서, 요약문 등을 문서로 저장할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "생성 문서 등록 성공",
                    content = @Content(schema = @Schema(implementation = DocumentDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 세션을 찾을 수 없습니다."),
            @ApiResponse(responseCode = "400", description = "잘못된 문서 데이터입니다.")
    })
    @PostMapping("/chat-sessions/{sessionId}/generated-documents")
    public ResponseEntity<List<DocumentDto.Response>> createGeneratedDocuments(
            @Parameter(description = "채팅 세션 ID", required = true, example = "1")
            @PathVariable Long sessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "생성된 문서 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ChatSessionDto.GeneratedDocumentRequest.class))
            )
            @RequestBody ChatSessionDto.GeneratedDocumentRequest request) {
        List<DocumentDto.Response> documents = chatSessionService.createGeneratedDocuments(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(documents);
    }
}
