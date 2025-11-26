package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.DashboardDto;
import com.twinsolution.construction.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "대시보드", description = "시스템 전체 현황을 요약하여 제공하는 대시보드 API입니다. 프로젝트, 문서, 채팅 세션 등의 통계 정보를 조회할 수 있습니다.")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "대시보드 요약 정보 조회",
            description = "시스템의 전체 현황을 요약하여 제공합니다. " +
                    "전체 프로젝트 수, 문서 수, 활성 채팅 세션 수, 최근 활동 등의 통계 정보를 한눈에 확인할 수 있습니다. " +
                    "대시보드 메인 화면에서 사용되는 핵심 지표들을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "대시보드 요약 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = DashboardDto.SummaryResponse.class))
            )
    })
    @GetMapping("/summary")
    public ResponseEntity<DashboardDto.SummaryResponse> getDashboardSummary() {
        DashboardDto.SummaryResponse summary = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }
}
