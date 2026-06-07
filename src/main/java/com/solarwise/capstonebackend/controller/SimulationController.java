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
    @PostMapping("/scenarios/power-high")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Anomaly>> triggerScenarioPowerHigh() {
        PowerAnomalyTriggerRequest request = PowerAnomalyTriggerRequest.builder()
                .plantId(1L)
                .anomalySeverity("HIGH")
                .differencePercentage(42.0)
                .durationHours(2)
                .description("시나리오 1: 인버터 장애로 인한 발전량 급락")
                .build();
        Anomaly anomaly = simulationService.triggerPowerAnomaly(request);
        return ResponseEntity.ok(ApiResponse.success(anomaly, "시나리오 1 (발전량 급락)이 성공적으로 트리거되었습니다."));
    }

    @Operation(summary = "▶️ 시나리오 2: 발전량 저하 (단순 경고)")
    @PostMapping("/scenarios/power-medium")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Anomaly>> triggerScenarioPowerMedium() {
        PowerAnomalyTriggerRequest request = PowerAnomalyTriggerRequest.builder()
                .plantId(1L)
                .anomalySeverity("MEDIUM")
                .differencePercentage(18.0)
                .durationHours(2)
                .description("시나리오 2: 패널 오염으로 인한 점진적 발전량 저하")
                .build();
        Anomaly anomaly = simulationService.triggerPowerAnomaly(request);
        return ResponseEntity.ok(ApiResponse.success(anomaly, "시나리오 2 (발전량 저하)가 성공적으로 트리거되었습니다."));
    }

    @Operation(summary = "▶️ 시나리오 3: 패널 크랙 감지 (긴급 알림 O)")
    @PostMapping("/scenarios/vision-crack")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Anomaly>> triggerScenarioVisionCrack() {
        VisionAnomalyTriggerRequest request = VisionAnomalyTriggerRequest.builder()
                .plantId(1L)
                .anomalyType("CRACK")
                .confidence(0.94)
                .imageUrl("/images/crack.jpg")
                .xaiExplanation("외부 충격으로 인한 선형 크랙 감지 (우측 상단 모서리)")
                .build();
        Anomaly anomaly = simulationService.triggerVisionAnomaly(request);
        return ResponseEntity.ok(ApiResponse.success(anomaly, "시나리오 3 (패널 크랙 감지)이 성공적으로 트리거되었습니다."));
    }

    @Operation(summary = "▶️ 시나리오 4: 패널 오염 감지 (단순 경고)")
    @PostMapping("/scenarios/vision-dirt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Anomaly>> triggerScenarioVisionDirt() {
        VisionAnomalyTriggerRequest request = VisionAnomalyTriggerRequest.builder()
                .plantId(1L)
                .anomalyType("DIRT")
                .confidence(0.75)
                .imageUrl("/images/pollution.jpg")
                .xaiExplanation("패널 하단부 조류 분변 및 먼지 누적 감지")
                .build();
        Anomaly anomaly = simulationService.triggerVisionAnomaly(request);
        return ResponseEntity.ok(ApiResponse.success(anomaly, "시나리오 4 (패널 오염 감지)가 성공적으로 트리거되었습니다."));
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
