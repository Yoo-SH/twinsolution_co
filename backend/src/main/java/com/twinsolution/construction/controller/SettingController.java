package com.twinsolution.construction.controller;

import com.twinsolution.construction.dto.SettingDto;
import com.twinsolution.construction.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SettingController {

    private final SettingService settingService;

    @GetMapping("/chunk")
    public ResponseEntity<SettingDto.Response> getChunkSettings() {
        SettingDto.Response settings = settingService.getChunkSettings();
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/chunk")
    public ResponseEntity<SettingDto.Response> updateChunkSettings(@RequestBody SettingDto.Request request) {
        SettingDto.Response response = settingService.updateChunkSettings(request);
        return ResponseEntity.ok(response);
    }
}
