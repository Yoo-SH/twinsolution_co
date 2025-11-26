package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.DocumentChunkDto;
import com.twinsolution.construction.service.DocumentChunkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "문서 청킹", description = "문서를 작은 단위(청크)로 분할하여 관리하는 API입니다. " +
        "청킹은 대용량 문서를 효율적으로 검색하고 AI가 처리하기 쉽게 만드는 기능입니다.")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentChunkController {

    private final DocumentChunkService documentChunkService;

    @Operation(
            summary = "문서의 청크 목록 조회",
            description = "특정 문서가 분할된 모든 청크(조각) 목록을 조회합니다. " +
                    "각 청크는 문서의 일부분을 포함하며, 순서와 내용을 확인할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "청크 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = DocumentChunkDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 문서를 찾을 수 없습니다.")
    })
    @GetMapping("/documents/{documentId}/chunks")
    public ResponseEntity<List<DocumentChunkDto.Response>> getChunksByDocument(
            @Parameter(description = "청크를 조회할 문서의 ID", required = true, example = "1")
            @PathVariable Long documentId) {
        List<DocumentChunkDto.Response> chunks = documentChunkService.getChunksByDocumentId(documentId);
        return ResponseEntity.ok(chunks);
    }

    @Operation(
            summary = "문서 청크 재생성",
            description = "문서의 청크를 삭제하고 새로운 설정으로 다시 생성합니다. " +
                    "청크 크기나 중복(overlap) 설정이 변경되었을 때 사용합니다. " +
                    "재생성 중에는 해당 문서를 AI 검색에 사용할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "청크 재생성 성공"
            ),
            @ApiResponse(responseCode = "404", description = "해당 문서를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "청크 생성 중 오류가 발생했습니다.")
    })
    @PostMapping("/documents/{documentId}/chunks/rebuild")
    public ResponseEntity<Void> rebuildChunks(
            @Parameter(description = "청크를 재생성할 문서의 ID", required = true, example = "1")
            @PathVariable Long documentId) {
        documentChunkService.rebuildChunks(documentId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "청크 설정 미리보기",
            description = "청킹 설정을 적용하기 전에 샘플 텍스트로 미리보기를 제공합니다. " +
                    "청크 크기, 중복 크기 등의 설정을 변경했을 때 실제 문서가 어떻게 분할될지 확인할 수 있습니다. " +
                    "설정을 저장하기 전에 테스트용으로 사용하세요."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "청크 미리보기 생성 성공",
                    content = @Content(schema = @Schema(implementation = DocumentChunkDto.PreviewResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 청크 설정 값입니다.")
    })
    @PostMapping("/settings/chunk/preview")
    public ResponseEntity<DocumentChunkDto.PreviewResponse> previewChunks(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "미리보기할 청크 설정 및 샘플 텍스트",
                    required = true,
                    content = @Content(schema = @Schema(implementation = DocumentChunkDto.PreviewRequest.class))
            )
            @RequestBody DocumentChunkDto.PreviewRequest request) {
        DocumentChunkDto.PreviewResponse response = documentChunkService.previewChunks(request);
        return ResponseEntity.ok(response);
    }
}
