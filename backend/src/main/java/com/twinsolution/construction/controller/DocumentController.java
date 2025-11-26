package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.DocumentDto;
import com.twinsolution.construction.service.DocumentService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "문서 관리", description = "프로젝트의 문서를 관리하는 API입니다. 문서 업로드, 조회, 삭제 기능을 제공합니다.")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(
            summary = "문서 업로드",
            description = "프로젝트에 새로운 문서를 업로드합니다. " +
                    "PDF, DOCX, TXT 등 다양한 형식의 파일을 지원합니다. " +
                    "업로드된 문서는 자동으로 청킹 처리되어 AI 검색 및 분석에 사용됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "문서 업로드 성공",
                    content = @Content(schema = @Schema(implementation = DocumentDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 파일 형식이거나 파일이 손상되었습니다."),
            @ApiResponse(responseCode = "500", description = "파일 업로드 중 오류가 발생했습니다.")
    })
    @PostMapping(value = "/projects/{projectId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentDto.Response> uploadDocument(
            @Parameter(description = "문서를 업로드할 프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "업로드할 문서 파일 (PDF, DOCX, TXT 등)", required = true)
            @RequestParam("file") MultipartFile file) throws IOException {
        DocumentDto.Response response = documentService.uploadDocument(projectId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "프로젝트의 문서 목록 조회",
            description = "특정 프로젝트에 속한 모든 문서 목록을 조회합니다. " +
                    "문서의 상태(처리 중, 완료 등)나 출처(업로드, AI 생성)로 필터링할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "문서 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = DocumentDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 프로젝트를 찾을 수 없습니다.")
    })
    @GetMapping("/projects/{projectId}/documents")
    public ResponseEntity<List<DocumentDto.Response>> getDocumentsByProject(
            @Parameter(description = "프로젝트 ID", required = true, example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "문서 상태 필터 (PROCESSING, COMPLETED 등)", example = "COMPLETED")
            @RequestParam(required = false) String status,
            @Parameter(description = "문서 출처 필터 (UPLOADED, GENERATED)", example = "UPLOADED")
            @RequestParam(required = false) String origin) {
        List<DocumentDto.Response> documents = documentService.getDocumentsByProjectId(projectId);
        return ResponseEntity.ok(documents);
    }

    @Operation(
            summary = "문서 상세 조회",
            description = "특정 문서의 상세 정보를 조회합니다. " +
                    "문서 메타데이터, 내용, 처리 상태, 청크 수 등을 확인할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "문서 조회 성공",
                    content = @Content(schema = @Schema(implementation = DocumentDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 문서를 찾을 수 없습니다.")
    })
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<DocumentDto.Response> getDocument(
            @Parameter(description = "조회할 문서의 ID", required = true, example = "1")
            @PathVariable Long documentId) {
        DocumentDto.Response document = documentService.getDocumentById(documentId);
        return ResponseEntity.ok(document);
    }

    @Operation(
            summary = "문서 삭제",
            description = "문서를 삭제합니다. 문서와 관련된 모든 청크, 분석 리포트 등도 함께 삭제됩니다. " +
                    "이 작업은 되돌릴 수 없으므로 주의하세요."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "문서 삭제 성공"
            ),
            @ApiResponse(responseCode = "404", description = "해당 문서를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "문서 삭제 중 오류가 발생했습니다.")
    })
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @Parameter(description = "삭제할 문서의 ID", required = true, example = "1")
            @PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}
