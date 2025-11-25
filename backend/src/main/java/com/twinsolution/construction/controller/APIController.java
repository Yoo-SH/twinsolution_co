package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.APIDto;
import com.twinsolution.construction.dto.APILogDto;
import com.twinsolution.construction.service.APIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/integration/apis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class APIController {

    private final APIService apiService;

    @PostMapping
    public ResponseEntity<APIDto.Response> createAPI(@RequestBody APIDto.Request request) {
        APIDto.Response response = apiService.createAPI(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<APIDto.Response>> getAllAPIs() {
        List<APIDto.Response> apis = apiService.getAllAPIs();
        return ResponseEntity.ok(apis);
    }

    @GetMapping("/{apiId}")
    public ResponseEntity<APIDto.Response> getAPI(@PathVariable Long apiId) {
        APIDto.Response api = apiService.getAPIById(apiId);
        return ResponseEntity.ok(api);
    }

    @PutMapping("/{apiId}")
    public ResponseEntity<APIDto.Response> updateAPI(
            @PathVariable Long apiId,
            @RequestBody APIDto.Request request) {
        APIDto.Response response = apiService.updateAPI(apiId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{apiId}/status")
    public ResponseEntity<APIDto.Response> updateAPIStatus(
            @PathVariable Long apiId,
            @RequestBody APIDto.StatusUpdateRequest request) {
        APIDto.Response response = apiService.updateAPIStatus(apiId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{apiId}/test-call")
    public ResponseEntity<APIDto.TestCallResponse> testAPICall(@PathVariable Long apiId) {
        APIDto.TestCallResponse response = apiService.testAPICall(apiId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{apiId}/logs")
    public ResponseEntity<List<APILogDto.Response>> getAPILogs(
            @PathVariable Long apiId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        List<APILogDto.Response> logs = apiService.getAPILogs(apiId, status, limit);
        return ResponseEntity.ok(logs);
    }
}
