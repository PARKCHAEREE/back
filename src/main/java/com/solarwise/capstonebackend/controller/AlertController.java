package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.AlertSettingDto;
import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// TODO: 알림 이력 조회 API 추가 필요
@Tag(name = "Alerts", description = "알림 설정 및 이력 관련 API")
@RestController
@RequestMapping("/api/v1/plants/{plantId}") // 명세서에 맞게 기본 경로 수정
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @Operation(summary = "알림 설정 조회", description = "현재 사용자의 알림 설정을 조회합니다.")
    @GetMapping("/alert-settings")
    public ResponseEntity<ApiResponse<AlertSettingDto>> getAlertSettings(
            @PathVariable Long plantId, // 경로 변수 추가
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        // plantId는 향후 발전소별 설정에 사용될 수 있으나, 현재 서비스는 사용자 단위이므로 userId만 사용
        AlertSettingDto settings = alertService.getAlertSetting(userId);
        return ResponseEntity.ok(ApiResponse.success(settings, "알림 설정 조회 성공"));
    }

    @Operation(summary = "알림 설정 수정", description = "현재 사용자의 알림 설정을 변경합니다.")
    @PutMapping("/alert-settings")
    public ResponseEntity<ApiResponse<AlertSettingDto>> updateAlertSettings(
            @PathVariable Long plantId, // 경로 변수 추가
            Authentication authentication,
            @RequestBody AlertSettingDto dto) {
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        // plantId는 향후 발전소별 설정에 사용될 수 있으나, 현재 서비스는 사용자 단위이므로 userId만 사용
        AlertSettingDto updatedSettings = alertService.updateAlertSetting(userId, dto);
        return ResponseEntity.ok(ApiResponse.success(updatedSettings, "알림 설정 변경 성공"));
    }
}
