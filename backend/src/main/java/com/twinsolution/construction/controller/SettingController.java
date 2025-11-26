package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.SettingDto;
import com.twinsolution.construction.service.SettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "시스템 설정", description = "시스템의 각종 설정을 관리하는 API입니다. 청킹 설정, AI 설정 등을 조회하고 수정할 수 있습니다.")
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SettingController {

    private final SettingService settingService;

    @Operation(
            summary = "청크 설정 조회",
            description = "문서 청킹과 관련된 시스템 설정을 조회합니다. " +
                    "청크 크기(chunk size), 청크 간 중복 크기(overlap size) 등의 설정값을 확인할 수 있습니다. " +
                    "이 설정은 새로 업로드되는 문서의 청킹 처리에 적용됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "청크 설정 조회 성공",
                    content = @Content(schema = @Schema(implementation = SettingDto.Response.class))
            )
    })
    @GetMapping("/chunk")
    public ResponseEntity<SettingDto.Response> getChunkSettings() {
        SettingDto.Response settings = settingService.getChunkSettings();
        return ResponseEntity.ok(settings);
    }

    @Operation(
            summary = "청크 설정 업데이트",
            description = "문서 청킹 설정을 업데이트합니다. " +
                    "청크 크기와 중복 크기를 조정할 수 있습니다. " +
                    "변경된 설정은 새로 업로드되는 문서부터 적용되며, 기존 문서에는 영향을 주지 않습니다. " +
                    "기존 문서에 새 설정을 적용하려면 청크 재생성 API를 사용하세요."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "청크 설정 업데이트 성공",
                    content = @Content(schema = @Schema(implementation = SettingDto.Response.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 설정 값입니다. (예: 청크 크기가 음수이거나 중복 크기가 청크 크기보다 큼)")
    })
    @PutMapping("/chunk")
    public ResponseEntity<SettingDto.Response> updateChunkSettings(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "업데이트할 청크 설정 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SettingDto.Request.class))
            )
            @RequestBody SettingDto.Request request) {
        SettingDto.Response response = settingService.updateChunkSettings(request);
        return ResponseEntity.ok(response);
    }
}
