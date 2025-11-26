package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.APIDto;
import com.twinsolution.construction.dto.APILogDto;
import com.twinsolution.construction.service.APIService;
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

@Tag(name = "API 연동 관리", description = "외부 API 연동을 관리하는 API입니다. API 등록, 조회, 수정, 테스트 호출 및 호출 로그 조회 기능을 제공합니다.")
@RestController
@RequestMapping("/api/integration/apis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class APIController {

    private final APIService apiService;

    @Operation(
            summary = "새로운 API 등록",
            description = "외부 API를 시스템에 등록합니다. API 이름, URL, HTTP 메서드, 헤더, 바디 등을 설정할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "API가 성공적으로 등록되었습니다.",
                    content = @Content(schema = @Schema(implementation = APIDto.Response.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터입니다.")
    })
    @PostMapping
    public ResponseEntity<APIDto.Response> createAPI(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "등록할 API 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = APIDto.Request.class))
            )
            @RequestBody APIDto.Request request) {
        APIDto.Response response = apiService.createAPI(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "전체 API 목록 조회",
            description = "시스템에 등록된 모든 API 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "API 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = APIDto.Response.class))
            )
    })
    @GetMapping
    public ResponseEntity<List<APIDto.Response>> getAllAPIs() {
        List<APIDto.Response> apis = apiService.getAllAPIs();
        return ResponseEntity.ok(apis);
    }

    @Operation(
            summary = "특정 API 상세 조회",
            description = "API ID를 통해 특정 API의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "API 조회 성공",
                    content = @Content(schema = @Schema(implementation = APIDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 ID의 API를 찾을 수 없습니다.")
    })
    @GetMapping("/{apiId}")
    public ResponseEntity<APIDto.Response> getAPI(
            @Parameter(description = "조회할 API의 ID", required = true, example = "1")
            @PathVariable Long apiId) {
        APIDto.Response api = apiService.getAPIById(apiId);
        return ResponseEntity.ok(api);
    }

    @Operation(
            summary = "API 정보 전체 수정",
            description = "등록된 API의 모든 정보를 수정합니다. 이름, URL, 메서드, 헤더, 바디 등을 변경할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "API 수정 성공",
                    content = @Content(schema = @Schema(implementation = APIDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 ID의 API를 찾을 수 없습니다."),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터입니다.")
    })
    @PutMapping("/{apiId}")
    public ResponseEntity<APIDto.Response> updateAPI(
            @Parameter(description = "수정할 API의 ID", required = true, example = "1")
            @PathVariable Long apiId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수정할 API 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = APIDto.Request.class))
            )
            @RequestBody APIDto.Request request) {
        APIDto.Response response = apiService.updateAPI(apiId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "API 상태 변경",
            description = "API의 활성화 상태를 변경합니다. API를 비활성화하면 해당 API는 더 이상 호출되지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "API 상태 변경 성공",
                    content = @Content(schema = @Schema(implementation = APIDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 ID의 API를 찾을 수 없습니다.")
    })
    @PatchMapping("/{apiId}/status")
    public ResponseEntity<APIDto.Response> updateAPIStatus(
            @Parameter(description = "상태를 변경할 API의 ID", required = true, example = "1")
            @PathVariable Long apiId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "변경할 상태 정보 (활성화/비활성화)",
                    required = true,
                    content = @Content(schema = @Schema(implementation = APIDto.StatusUpdateRequest.class))
            )
            @RequestBody APIDto.StatusUpdateRequest request) {
        APIDto.Response response = apiService.updateAPIStatus(apiId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "API 테스트 호출",
            description = "등록된 API를 실제로 호출하여 테스트합니다. 응답 시간, 상태 코드, 응답 데이터 등을 확인할 수 있으며, 호출 결과는 로그에 자동으로 기록됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "API 호출 성공 (API 호출 자체의 성공 여부와 무관하게 테스트가 수행되면 200 반환)",
                    content = @Content(schema = @Schema(implementation = APIDto.TestCallResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 ID의 API를 찾을 수 없습니다.")
    })
    @PostMapping("/{apiId}/test-call")
    public ResponseEntity<APIDto.TestCallResponse> testAPICall(
            @Parameter(description = "테스트 호출할 API의 ID", required = true, example = "1")
            @PathVariable Long apiId) {
        APIDto.TestCallResponse response = apiService.testAPICall(apiId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "API 호출 로그 조회",
            description = "특정 API의 호출 이력을 조회합니다. 호출 시간, 응답 시간, 상태 코드, 성공/실패 여부 등을 확인할 수 있습니다. " +
                    "상태별 필터링과 조회 개수 제한이 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "API 로그 조회 성공",
                    content = @Content(schema = @Schema(implementation = APILogDto.Response.class))
            ),
            @ApiResponse(responseCode = "404", description = "해당 ID의 API를 찾을 수 없습니다.")
    })
    @GetMapping("/{apiId}/logs")
    public ResponseEntity<List<APILogDto.Response>> getAPILogs(
            @Parameter(description = "로그를 조회할 API의 ID", required = true, example = "1")
            @PathVariable Long apiId,
            @Parameter(description = "필터링할 상태 (SUCCESS, FAILED 등)", example = "SUCCESS")
            @RequestParam(required = false) String status,
            @Parameter(description = "조회할 로그 개수 (기본값: 50)", example = "50")
            @RequestParam(defaultValue = "50") int limit) {
        List<APILogDto.Response> logs = apiService.getAPILogs(apiId, status, limit);
        return ResponseEntity.ok(logs);
    }
}
