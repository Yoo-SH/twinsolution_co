package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.ChatMessageDto;
import com.twinsolution.construction.dto.ProjectDto;
import com.twinsolution.construction.service.ChatMessageService;
import com.twinsolution.construction.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Tag(name = "프로젝트 관리", description = "건설 프로젝트를 관리하는 API입니다. 프로젝트 생성, 조회, 수정, 삭제 및 프로젝트별 AI 챗봇 대화 기능을 제공합니다.")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;
    private final ChatMessageService chatMessageService;

    @Operation(
            summary = "새로운 프로젝트 생성",
            description = "새로운 건설 프로젝트를 생성합니다. " +
                    "프로젝트 이름, 설명, 시작일, 종료일 등의 정보를 설정할 수 있습니다. " +
                    "생성된 프로젝트에는 문서를 업로드하고 AI 챗봇 세션을 시작할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "프로젝트 생성 성공",
                    content = @Content(schema = @Schema(implementation = ProjectDto.Response.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 프로젝트 정보입니다.")
    })
    @PostMapping
    public ResponseEntity<ProjectDto.Response> createProject(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "생성할 프로젝트 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ProjectDto.Request.class))
            )
            @RequestBody ProjectDto.Request request) {
        ProjectDto.Response response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "최근 프로젝트 목록 조회",
            description = "최근에 생성되거나 업데이트된 프로젝트 목록을 조회합니다. " +
                    "기본적으로 최근 5개의 프로젝트를 반환하며, limit 파라미터로 조회 개수를 조정할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "프로젝트 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ProjectDto.Response.class))
            )
    })
    @GetMapping
    public ResponseEntity<List<ProjectDto.Response>> getRecentProjects(
            @Parameter(description = "조회할 프로젝트 수 (기본값: 5)", example = "5")
            @RequestParam(defaultValue = "5") int limit) {
        List<ProjectDto.Response> projects = projectService.getRecentProjects(limit);
        return ResponseEntity.ok(projects);
    }

    @Operation(
            summary = "프로젝트 상세 조회",
            description = "특정 프로젝트의 상세 정보를 조회합니다. " +
                    "프로젝트 기본 정보, 통계(문서 수, 채팅 세션 수 등) 등을 확인할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "프로젝트 조회 성공",
                    content = @Content(schema = @Schema(implementation = ProjectDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다.")
    })
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDto.Response> getProject(
            @Parameter(description = "조회할 프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId) {
        ProjectDto.Response project = projectService.getProjectById(projectId);
        return ResponseEntity.ok(project);
    }

    @Operation(
            summary = "프로젝트 정보 부분 수정",
            description = "프로젝트의 일부 정보를 수정합니다. " +
                    "프로젝트 이름, 설명, 상태 등을 변경할 수 있으며, 수정하지 않을 필드는 기존 값이 유지됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "프로젝트 수정 성공",
                    content = @Content(schema = @Schema(implementation = ProjectDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "400", description = "잘못된 수정 정보입니다.")
    })
    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectDto.Response> updateProject(
            @Parameter(description = "수정할 프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수정할 프로젝트 정보 (변경할 필드만 포함)",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ProjectDto.UpdateRequest.class))
            )
            @RequestBody ProjectDto.UpdateRequest request) {
        ProjectDto.Response response = projectService.updateProject(projectId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "프로젝트 삭제",
            description = "프로젝트를 삭제합니다. " +
                    "프로젝트에 속한 모든 문서, 채팅 세션, 분석 리포트 등이 함께 삭제됩니다. " +
                    "이 작업은 되돌릴 수 없으므로 주의하세요."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "프로젝트 삭제 성공"
            ),
            @ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "프로젝트 삭제 중 오류가 발생했습니다.")
    })
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @Parameter(description = "삭제할 프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 프로젝트별 AI 채팅 메시지 관리 ====================

    @Operation(
            summary = "프로젝트 채팅 메시지 목록 조회",
            description = "특정 프로젝트의 전체 대화 이력을 조회합니다. " +
                    "프로젝트마다 하나의 세션이 자동으로 생성되며, 사용자와 AI의 모든 대화 내용을 시간순으로 확인할 수 있습니다. " +
                    "페이지네이션 파라미터를 사용하여 메시지를 나누어 조회할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "메시지 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ChatMessageDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다.")
    })
    @GetMapping("/{projectId}/messages")
    public ResponseEntity<List<ChatMessageDto.Response>> getProjectMessages(
            @Parameter(description = "프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "페이지 번호 (0부터 시작, 선택사항)", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지당 메시지 수 (선택사항)", example = "20")
            @RequestParam(required = false) Integer size) {

        Long sessionId = projectService.getOrCreateSessionForProject(projectId);

        List<ChatMessageDto.Response> messages;
        if (page != null && size != null) {
            messages = chatMessageService.getMessagesBySessionId(sessionId, page, size);
        } else {
            messages = chatMessageService.getMessagesBySessionId(sessionId);
        }
        return ResponseEntity.ok(messages);
    }

    @Operation(
            summary = "프로젝트에 메시지 전송 및 AI 응답 받기 (OpenAI 통합)",
            description = "프로젝트에 사용자 메시지를 전송하고 OpenAI API를 통해 AI 응답을 받습니다.\n\n" +
                    "**주요 기능:**\n" +
                    "- OpenAI GPT 모델을 사용한 자연어 대화\n" +
                    "- 시스템 프롬프트로 AI 역할 및 행동 커스터마이징\n" +
                    "- 모델 선택 (gpt-3.5-turbo, gpt-4, gpt-4-turbo, gpt-4o)\n" +
                    "- Temperature 조절로 응답의 창의성/일관성 제어\n" +
                    "- 최대 토큰 수 제한 설정\n\n" +
                    "**요청 예시:**\n" +
                    "```json\n" +
                    "{\n" +
                    "  \"content\": \"안녕하세요, 건설 프로젝트에 대해 질문이 있습니다.\",\n" +
                    "  \"systemPrompt\": \"당신은 건설 전문가입니다. 건설, 건축 자재, 안전 규정에 대한 정확한 답변을 제공하세요.\",\n" +
                    "  \"model\": \"gpt-3.5-turbo\",\n" +
                    "  \"temperature\": 0.7,\n" +
                    "  \"maxTokens\": 1000\n" +
                    "}\n" +
                    "```\n\n" +
                    "**참고사항:**\n" +
                    "- content (필수): 사용자가 전송할 메시지\n" +
                    "- systemPrompt (선택): AI의 역할 및 행동 지시\n" +
                    "- model (선택): OpenAI 모델 (기본값: gpt-3.5-turbo)\n" +
                    "- temperature (선택): 0.0~2.0, 낮을수록 일관적, 높을수록 창의적 (기본값: 0.7)\n" +
                    "- maxTokens (선택): 응답의 최대 토큰 수\n" +
                    "- stream (선택): 스트리밍 모드 활성화 (기본값: false)\n" +
                    "  - false: 전체 응답을 JSON으로 반환 (application/json)\n" +
                    "  - true: 응답을 실시간으로 스트리밍 (text/event-stream)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "메시지 전송 및 AI 응답 생성 성공\n\n" +
                            "- stream=false: JSON 형식으로 전체 응답 반환\n" +
                            "- stream=true: Server-Sent Events로 실시간 스트리밍",
                    content = @Content(schema = @Schema(implementation = ChatMessageDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "400", description = "잘못된 메시지 형식입니다. content 필드는 필수입니다."),
            @ApiResponse(responseCode = "401", description = "OpenAI API 인증 실패. API 키를 확인하세요."),
            @ApiResponse(responseCode = "500", description = "OpenAI API 호출 중 오류가 발생했습니다.")
    })
    @PostMapping(value = "/{projectId}/messages", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Object sendProjectMessage(
            @Parameter(description = "프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "사용자 메시지 및 OpenAI 설정 옵션\n\n" +
                            "**필수 필드:**\n" +
                            "- content: 사용자 메시지\n\n" +
                            "**선택 필드:**\n" +
                            "- systemPrompt: AI 역할 지시\n" +
                            "- model: OpenAI 모델 선택\n" +
                            "- temperature: 응답 창의성 조절\n" +
                            "- maxTokens: 최대 토큰 수\n" +
                            "- stream: 스트리밍 모드 (true/false)",
                    required = true,
                    content = @Content(
                            schema = @Schema(
                                    implementation = ChatMessageDto.Request.class,
                                    example = "{\n" +
                                            "  \"content\": \"안녕하세요, 건설 프로젝트에 대해 질문이 있습니다.\",\n" +
                                            "  \"systemPrompt\": \"당신은 건설 전문가입니다.\",\n" +
                                            "  \"model\": \"gpt-3.5-turbo\",\n" +
                                            "  \"temperature\": 0.7,\n" +
                                            "  \"maxTokens\": 1000\n" +
                                            "}"
                            )
                    )
            )
            @RequestBody ChatMessageDto.Request request) {

        Long sessionId = projectService.getOrCreateSessionForProject(projectId);

        // stream 옵션이 true인 경우 스트리밍 응답 반환
        if (Boolean.TRUE.equals(request.getStream())) {
            SseEmitter emitter = chatMessageService.sendMessageStreamWithEmitter(sessionId, request);
            return emitter;
        }

        // stream 옵션이 false이거나 null인 경우 일반 응답 반환
        ChatMessageDto.Response response = chatMessageService.sendMessage(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
