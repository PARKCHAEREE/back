package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.PowerAnomalyTriggerRequest;
import com.solarwise.capstonebackend.dto.SimulationPlaybackStatusDto;
import com.solarwise.capstonebackend.dto.VisionAnomalyTriggerRequest;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "Simulation API", description = "가상 시간 제어 및 시나리오 트리거 API")
@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @Operation(summary = "▶️ 시나리오 1: 발전량 급락 (긴급 알림 O)")
    @PostMapping("/trigger-power-anomaly")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Anomaly>> triggerPowerAnomaly(@RequestBody PowerAnomalyTriggerRequest request) {
        Anomaly anomaly = simulationService.triggerPowerAnomaly(request);
        return ResponseEntity.ok(ApiResponse.success(anomaly, "발전량 이상 시나리오가 성공적으로 트리거되었습니다."));
    }

    @Operation(summary = "▶️ 시나리오 3: 패널 이상 감지 (비전 AI)")
    @PostMapping("/trigger-vision-anomaly")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Anomaly>> triggerVisionAnomaly(@RequestBody VisionAnomalyTriggerRequest request) {
        Anomaly anomaly = simulationService.triggerVisionAnomaly(request);
        return ResponseEntity.ok(ApiResponse.success(anomaly, "비전 이상 시나리오가 성공적으로 트리거되었습니다."));
    }

    @Operation(summary = "가상 현재 시간 조회")
    @GetMapping("/time")
    public ResponseEntity<ApiResponse<LocalDateTime>> getVirtualTime() {
        return ResponseEntity.ok(ApiResponse.success(simulationService.getVirtualCurrentTime(), "가상 현재 시간 조회 성공"));
    }

    @Operation(summary = "가상 시간 1시간 전진 (수동)")
    @PostMapping("/tick")
    public ResponseEntity<ApiResponse<LocalDateTime>> advanceTime() {
        simulationService.advanceTimeByHour();
        return ResponseEntity.ok(ApiResponse.success(simulationService.getVirtualCurrentTime(), "가상 시간 1시간 전진 완료"));
    }

    @Operation(summary = "시뮬레이션 자동 재생 시작")
    @PostMapping("/playback/start")
    public ResponseEntity<ApiResponse<SimulationPlaybackStatusDto>> startPlayback() {
        simulationService.startPlayback();
        return ResponseEntity.ok(ApiResponse.success(buildPlaybackStatus(), "시뮬레이션 자동 재생 시작"));
    }

    @Operation(summary = "시뮬레이션 자동 재생 정지")
    @PostMapping("/playback/stop")
    public ResponseEntity<ApiResponse<SimulationPlaybackStatusDto>> stopPlayback() {
        simulationService.stopPlayback();
        return ResponseEntity.ok(ApiResponse.success(buildPlaybackStatus(), "시뮬레이션 자동 재생 정지"));
    }

    @Operation(summary = "시뮬레이션 자동 재생 상태 조회")
    @GetMapping("/playback/status")
    public ResponseEntity<ApiResponse<SimulationPlaybackStatusDto>> getPlaybackStatus() {
        return ResponseEntity.ok(ApiResponse.success(buildPlaybackStatus(), "시뮬레이션 자동 재생 상태 조회 성공"));
    }

    private SimulationPlaybackStatusDto buildPlaybackStatus() {
        return SimulationPlaybackStatusDto.builder()
                .running(simulationService.isPlaybackRunning())
                .virtualCurrentTime(simulationService.getVirtualCurrentTime())
                .build();
    }
}
