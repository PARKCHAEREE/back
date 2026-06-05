package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.PowerAnomalyTriggerRequest;
import com.solarwise.capstonebackend.dto.AnomalyDto;
import com.solarwise.capstonebackend.dto.SimulationPlaybackStatusDto;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "Simulation API", description = "가상 시간 기반 시뮬레이션 제어")
@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @Operation(summary = "가상 현재 시간 조회")
    @GetMapping("/time")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LocalDateTime>> getVirtualTime() {
        LocalDateTime currentTime = simulationService.getVirtualCurrentTime();
        return ResponseEntity.ok(ApiResponse.success(currentTime, "가상 현재 시간 조회 성공"));
    }

    @Operation(summary = "가상 시간 1시간 전진 (백업용 수동 스텝)")
    @PostMapping("/tick")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LocalDateTime>> advanceTime() {
        simulationService.advanceTimeByHour();
        LocalDateTime updatedTime = simulationService.getVirtualCurrentTime();
        return ResponseEntity.ok(ApiResponse.success(updatedTime, "가상 시간 1시간 전진 완료"));
    }

    @Operation(summary = "시뮬레이션 자동 재생 시작 (1초 고정)")
    @PostMapping("/playback/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SimulationPlaybackStatusDto>> startPlayback() {
        simulationService.startPlayback();
        return ResponseEntity.ok(ApiResponse.success(buildPlaybackStatus(), "시뮬레이션 자동 재생 시작"));
    }

    @Operation(summary = "시뮬레이션 자동 재생 정지")
    @PostMapping("/playback/stop")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SimulationPlaybackStatusDto>> stopPlayback() {
        simulationService.stopPlayback();
        return ResponseEntity.ok(ApiResponse.success(buildPlaybackStatus(), "시뮬레이션 자동 재생 정지"));
    }

    @Operation(summary = "시뮬레이션 자동 재생 상태 조회")
    @GetMapping("/playback/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SimulationPlaybackStatusDto>> getPlaybackStatus() {
        return ResponseEntity.ok(ApiResponse.success(buildPlaybackStatus(), "시뮬레이션 자동 재생 상태 조회 성공"));
    }

    @Operation(summary = "드론 에러 트리거 설정")
    @PostMapping("/trigger-drone-error")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> triggerDroneError() {
        simulationService.triggerDroneError();
        return ResponseEntity.ok(ApiResponse.success("드론 에러 트리거 설정됨", "다음 스케줄 실행 시 파손 패널 이미지 분석이 수행됩니다."));
    }

    @Operation(summary = "시뮬레이션: 발전량 이상 트리거")
    @PostMapping("/trigger-power-anomaly")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnomalyDto>> triggerPowerAnomaly(@RequestBody PowerAnomalyTriggerRequest request) {
        Anomaly anomaly = simulationService.triggerPowerAnomaly(request);
        return ResponseEntity.ok(ApiResponse.success(toAnomalyDto(anomaly), "발전량 이상 시뮬레이션 트리거 생성됨"));
    }

    @Operation(summary = "시뮬레이션: 비전 이상 트리거")
    @PostMapping("/trigger-vision-anomaly")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnomalyDto>> triggerVisionAnomaly(@RequestBody com.solarwise.capstonebackend.dto.VisionAnomalyTriggerRequest request) {
        Anomaly anomaly = simulationService.triggerVisionAnomaly(request);
        return ResponseEntity.ok(ApiResponse.success(toAnomalyDto(anomaly), "비전 이상 시뮬레이션 트리거 생성됨"));
    }

    private SimulationPlaybackStatusDto buildPlaybackStatus() {
        return SimulationPlaybackStatusDto.builder()
                .running(simulationService.isPlaybackRunning())
                .tickSeconds(simulationService.getPlaybackTickSeconds())
                .stepHours(simulationService.getPlaybackStepHours())
                .virtualCurrentTime(simulationService.getVirtualCurrentTime())
                .lastTickAt(simulationService.getLastTickAt())
                .build();
    }

    private AnomalyDto toAnomalyDto(Anomaly anomaly) {
        return AnomalyDto.builder()
                .eventId(anomaly.getId())
                .type(anomaly.getType())
                .severity(anomaly.getSeverity())
                .detectedAt(anomaly.getDetectedAt())
                .summary(anomaly.getSummary())
                .status(anomaly.getStatus())
                .cause(anomaly.getCause())
                .recommendedAction(anomaly.getRecommendedAction())
                .xaiExplanation(anomaly.getXaiExplanation())
                .build();
    }
}
