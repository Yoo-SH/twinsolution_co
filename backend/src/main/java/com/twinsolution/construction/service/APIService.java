package com.twinsolution.construction.service;

import com.twinsolution.construction.dto.APIDto;
import com.twinsolution.construction.dto.APILogDto;
import com.twinsolution.construction.entity.API;
import com.twinsolution.construction.entity.APILog;
import com.twinsolution.construction.exception.ResourceNotFoundException;
import com.twinsolution.construction.repository.APILogRepository;
import com.twinsolution.construction.repository.APIRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class APIService {

    private final APIRepository apiRepository;
    private final APILogRepository apiLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public APIDto.Response createAPI(APIDto.Request request) {
        API api = API.builder()
                .name(request.getName())
                .baseUrl(request.getBaseUrl())
                .method(request.getMethod())
                .authKey(request.getAuthKey())
                .status(request.getStatus() != null ? request.getStatus() : "active")
                .build();

        API savedAPI = apiRepository.save(api);
        return convertToResponse(savedAPI);
    }

    public List<APIDto.Response> getAllAPIs() {
        List<API> apis = apiRepository.findAllByOrderByCreatedAtDesc();
        return apis.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public APIDto.Response getAPIById(Long apiId) {
        API api = apiRepository.findById(apiId)
                .orElseThrow(() -> new ResourceNotFoundException("API", "ID", apiId));
        return convertToResponse(api);
    }

    @Transactional
    public APIDto.Response updateAPI(Long apiId, APIDto.Request request) {
        API api = apiRepository.findById(apiId)
                .orElseThrow(() -> new ResourceNotFoundException("API", "ID", apiId));

        if (request.getName() != null) api.setName(request.getName());
        if (request.getBaseUrl() != null) api.setBaseUrl(request.getBaseUrl());
        if (request.getMethod() != null) api.setMethod(request.getMethod());
        if (request.getAuthKey() != null) api.setAuthKey(request.getAuthKey());
        if (request.getStatus() != null) api.setStatus(request.getStatus());

        return convertToResponse(api);
    }

    @Transactional
    public APIDto.Response updateAPIStatus(Long apiId, APIDto.StatusUpdateRequest request) {
        API api = apiRepository.findById(apiId)
                .orElseThrow(() -> new ResourceNotFoundException("API", "ID", apiId));

        api.setStatus(request.getStatus());
        return convertToResponse(api);
    }

    @Transactional
    public APIDto.TestCallResponse testAPICall(Long apiId) {
        API api = apiRepository.findById(apiId)
                .orElseThrow(() -> new ResourceNotFoundException("API", "ID", apiId));

        try {
            String response = restTemplate.getForObject(api.getBaseUrl(), String.class);

            logAPICall(api, "success", 200, "Test call successful");

            return APIDto.TestCallResponse.builder()
                    .status("success")
                    .statusCode(200)
                    .message("Test call successful")
                    .responseBody(response)
                    .build();
        } catch (Exception e) {
            logAPICall(api, "error", 500, e.getMessage());

            return APIDto.TestCallResponse.builder()
                    .status("error")
                    .statusCode(500)
                    .message(e.getMessage())
                    .responseBody(null)
                    .build();
        }
    }

    public List<APILogDto.Response> getAPILogs(Long apiId, String status, int limit) {
        List<APILog> logs;
        if (status != null) {
            logs = apiLogRepository.findByApiIdAndStatusOrderByCreatedAtDesc(apiId, status, PageRequest.of(0, limit));
        } else {
            logs = apiLogRepository.findByApiIdOrderByCreatedAtDesc(apiId, PageRequest.of(0, limit));
        }

        return logs.stream()
                .map(this::convertLogToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    private void logAPICall(API api, String status, int statusCode, String message) {
        APILog log = APILog.builder()
                .api(api)
                .status(status)
                .statusCode(statusCode)
                .message(message)
                .build();

        apiLogRepository.save(log);
    }

    private APIDto.Response convertToResponse(API api) {
        return APIDto.Response.builder()
                .id(api.getId())
                .name(api.getName())
                .baseUrl(api.getBaseUrl())
                .method(api.getMethod())
                .authKey(api.getAuthKey())
                .status(api.getStatus())
                .createdAt(api.getCreatedAt())
                .updatedAt(api.getUpdatedAt())
                .build();
    }

    private APILogDto.Response convertLogToResponse(APILog log) {
        return APILogDto.Response.builder()
                .id(log.getId())
                .apiId(log.getApi().getId())
                .status(log.getStatus())
                .statusCode(log.getStatusCode())
                .message(log.getMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
