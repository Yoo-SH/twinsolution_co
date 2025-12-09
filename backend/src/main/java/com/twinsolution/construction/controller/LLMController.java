package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.LLMDto;
import com.twinsolution.construction.service.LLMService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "LLM", description = "LLM Provider 관리 API")
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LLMController {

    private final LLMService llmService;

    @Operation(summary = "사용 가능한 LLM Provider 목록 조회",
            description = "OpenAI와 Ollama 등 사용 가능한 모든 LLM Provider와 지원 모델 목록을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 Provider 목록을 반환")
    })
    @GetMapping("/providers")
    public ResponseEntity<LLMDto.ProvidersResponse> getAvailableProviders() {
        LLMDto.ProvidersResponse response = llmService.getAvailableProviders();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Ollama 가용성 및 모델 목록 조회",
            description = "로컬 Ollama 서비스의 가용성과 설치된 모델 목록을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ollama 가용성 정보 반환")
    })
    @GetMapping("/ollama/available")
    public ResponseEntity<LLMDto.OllamaAvailabilityResponse> getOllamaAvailability() {
        LLMDto.OllamaAvailabilityResponse response = llmService.getOllamaAvailability();
        return ResponseEntity.ok(response);
    }
}
