package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.AnalysisReportDto;
import com.twinsolution.construction.service.AnalysisReportService;
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

@Tag(name = "문서 분석 리포트", description = "AI를 활용한 문서 분석 및 리포트 생성 API입니다. 업로드된 문서를 AI가 자동으로 분석하여 요약, 키워드 추출, 주요 내용 등을 제공합니다.")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalysisReportController {

    private final AnalysisReportService analysisReportService;

    @Operation(
            summary = "문서 분석 리포트 생성",
            description = "특정 문서에 대한 AI 기반 분석 리포트를 생성합니다. " +
                    "AI가 문서의 내용을 분석하여 주요 내용, 키워드, 요약 등을 자동으로 추출합니다. " +
                    "분석에는 시간이 소요될 수 있으며, 생성된 리포트는 저장되어 나중에 조회할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "분석 리포트가 성공적으로 생성되었습니다.",
                    content = @Content(schema = @Schema(implementation = AnalysisReportDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 ID의 문서를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "500", description = "AI 분석 중 오류가 발생했습니다.")
    })
    @PostMapping("/{documentId}/analysis")
    public ResponseEntity<AnalysisReportDto.Response> createAnalysisReport(
            @Parameter(description = "분석할 문서의 ID", required = true, example = "1")
            @PathVariable Long documentId) {
        AnalysisReportDto.Response response = analysisReportService.createAnalysisReport(documentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "문서의 전체 분석 리포트 목록 조회",
            description = "특정 문서에 대해 생성된 모든 분석 리포트 목록을 조회합니다. " +
                    "하나의 문서에 대해 여러 번 분석을 수행한 경우, 모든 분석 이력을 확인할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "분석 리포트 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = AnalysisReportDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 ID의 문서를 찾을 수 없습니다.")
    })
    @GetMapping("/{documentId}/analysis")
    public ResponseEntity<List<AnalysisReportDto.Response>> getAnalysisReports(
            @Parameter(description = "조회할 문서의 ID", required = true, example = "1")
            @PathVariable Long documentId) {
        List<AnalysisReportDto.Response> reports = analysisReportService.getAnalysisReportsByDocumentId(documentId);
        return ResponseEntity.ok(reports);
    }

    @Operation(
            summary = "특정 분석 리포트 상세 조회",
            description = "문서의 특정 분석 리포트 상세 정보를 조회합니다. " +
                    "분석 결과의 전체 내용, 키워드, 요약문 등을 확인할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "분석 리포트 조회 성공",
                    content = @Content(schema = @Schema(implementation = AnalysisReportDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 문서 또는 리포트를 찾을 수 없습니다.")
    })
    @GetMapping("/{documentId}/analysis/{reportId}")
    public ResponseEntity<AnalysisReportDto.Response> getAnalysisReport(
            @Parameter(description = "문서 ID", required = true, example = "1")
            @PathVariable Long documentId,
            @Parameter(description = "조회할 리포트 ID", required = true, example = "1")
            @PathVariable Long reportId) {
        AnalysisReportDto.Response report = analysisReportService.getAnalysisReportById(documentId, reportId);
        return ResponseEntity.ok(report);
    }
}
