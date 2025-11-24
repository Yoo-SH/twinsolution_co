package com.twinsolution.construction.service;

import com.twinsolution.construction.dto.SettingDto;
import com.twinsolution.construction.entity.Setting;
import com.twinsolution.construction.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingService {

    private final SettingRepository settingRepository;

    public SettingDto.Response getChunkSettings() {
        Setting setting = settingRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> createDefaultSettings());

        return convertToResponse(setting);
    }

    @Transactional
    public SettingDto.Response updateChunkSettings(SettingDto.Request request) {
        Setting setting = settingRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> createDefaultSettings());

        if (request.getChunkSize() != null) {
            setting.setChunkSize(request.getChunkSize());
        }
        if (request.getChunkOverlap() != null) {
            setting.setChunkOverlap(request.getChunkOverlap());
        }

        Setting savedSetting = settingRepository.save(setting);
        return convertToResponse(savedSetting);
    }

    @Transactional
    private Setting createDefaultSettings() {
        Setting defaultSetting = Setting.builder()
                .chunkSize(1000)
                .chunkOverlap(200)
                .build();

        return settingRepository.save(defaultSetting);
    }

    private SettingDto.Response convertToResponse(Setting setting) {
        return SettingDto.Response.builder()
                .id(setting.getId())
                .chunkSize(setting.getChunkSize())
                .chunkOverlap(setting.getChunkOverlap())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }
}
