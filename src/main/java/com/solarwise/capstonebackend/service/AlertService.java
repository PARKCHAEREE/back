package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.AlertSettingDto;
import com.solarwise.capstonebackend.entity.AlertSetting;
import com.solarwise.capstonebackend.entity.User;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.AlertSettingRepository;
import com.solarwise.capstonebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertSettingRepository alertSettingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AlertSettingDto getAlertSetting(Long userId) {
        AlertSetting setting = findOrCreateSetting(userId);
        return toDto(setting);
    }

    @Transactional
    public AlertSettingDto updateAlertSetting(Long userId, AlertSettingDto dto) {
        AlertSetting setting = findOrCreateSetting(userId);
        setting.setEmailEnabled(dto.isEmailEnabled());
        setting.setSmsEnabled(dto.isSmsEnabled());
        setting.setMinimumSeverity(dto.getMinimumSeverity());
        AlertSetting updatedSetting = alertSettingRepository.save(setting);
        return toDto(updatedSetting);
    }

    private AlertSetting findOrCreateSetting(Long userId) {
        return alertSettingRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
            AlertSetting newSetting = AlertSetting.builder().user(user).build();
            return alertSettingRepository.save(newSetting);
        });
    }

    private AlertSettingDto toDto(AlertSetting entity) {
        return AlertSettingDto.builder()
                .emailEnabled(entity.isEmailEnabled())
                .smsEnabled(entity.isSmsEnabled())
                .minimumSeverity(entity.getMinimumSeverity())
                .build();
    }
}
