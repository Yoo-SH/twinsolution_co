package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.ProjectDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "프로젝트 관리", description = "건설 프로젝트를 관리하는 API입니다. 프로젝트 생성, 조회, 수정, 삭제 기능을 제공합니다.")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;

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
}
