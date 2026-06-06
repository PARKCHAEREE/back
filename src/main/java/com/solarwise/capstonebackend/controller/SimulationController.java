package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.PowerAnomalyTriggerRequest;
import com.solarwise.capstonebackend.dto.VisionAnomalyTriggerRequest;
import com.solarwise.capstonebackend.dto.SimulationPlaybackStatusDto;
import com.solarwise.capstonebackend.service.SimulationService;
import io.swagger.v3.oas.annotations.Hidden;
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

    // --- 시나리오 실행 버튼 API ---

    @Operation(summary = "▶️ 시나리오 1: 발전량 급락 (긴급 알림 O)", description = "발전량이 42% 급락하여 즉시 이메일 알림이 발송되는 시나리오를 실행합니다.")
    @PostMapping("/scenarios/power-high")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> triggerScenarioPowerHigh() {
        PowerAnomalyTriggerRequest request = PowerAnomalyTriggerRequest.builder()
                .plantId(1L)
                .anomalySeverity("HIGH")
                .differencePercentage(42.0)
                .durationHours(2)
                .description("시나리오 1: 인버터 장애로 인한 발전량 급락")
                .build();
        simulationService.triggerPowerAnomaly(request);
        return ResponseEntity.ok(ApiResponse.success("시나리오 1 (발전량 급락)이 성공적으로 트리거되었습니다."));
    }

    @Operation(summary = "▶️ 시나리오 2: 발전량 저하 (단순 경고)", description = "발전량이 18% 저하되어 대시보드에 경고만 표시되는 시나리오를 실행합니다.")
    @PostMapping("/scenarios/power-medium")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> triggerScenarioPowerMedium() {
        PowerAnomalyTriggerRequest request = PowerAnomalyTriggerRequest.builder()
                .plantId(1L)
                .anomalySeverity("MEDIUM")
                .differencePercentage(18.0)
                .durationHours(2)
                .description("시나리오 2: 패널 오염으로 인한 점진적 발전량 저하")
                .build();
        simulationService.triggerPowerAnomaly(request);
        return ResponseEntity.ok(ApiResponse.success("시나리오 2 (발전량 저하)가 성공적으로 트리거되었습니다."));
    }

    @Operation(summary = "▶️ 시나리오 3: 패널 크랙 감지 (긴급 알림 O)", description = "드론이 패널 파손을 발견하여 즉시 이메일 알림이 발송되는 시나리오를 실행합니다.")
    @PostMapping("/scenarios/vision-crack")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> triggerScenarioVisionCrack() {
        VisionAnomalyTriggerRequest request = VisionAnomalyTriggerRequest.builder()
                .plantId(1L)
                .anomalyType("CRACK")
                .anomalySeverity("HIGH")
                .confidence(0.94)
                .imageUrl("http://localhost:8080/images/crack.jpg")
                .xaiExplanation("외부 충격으로 인한 선형 크랙 감지 (우측 상단 모서리)")
                .build();
        simulationService.triggerVisionAnomaly(request);
        return ResponseEntity.ok(ApiResponse.success("시나리오 3 (패널 크랙 감지)이 성공적으로 트리거되었습니다."));
    }

    @Operation(summary = "▶️ 시나리오 4: 패널 오염 감지 (단순 경고)", description = "드론이 패널 오염을 발견하여 대시보드에 경고만 표시되는 시나리오를 실행합니다.")
    @PostMapping("/scenarios/vision-dirt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> triggerScenarioVisionDirt() {
        VisionAnomalyTriggerRequest request = VisionAnomalyTriggerRequest.builder()
                .plantId(1L)
                .anomalyType("DIRT")
                .anomalySeverity("MEDIUM")
                .confidence(0.75)
                .imageUrl("http://localhost:8080/images/pollution.jpg")
                .xaiExplanation("패널 하단부 조류 분변 및 먼지 누적 감지")
                .build();
        simulationService.triggerVisionAnomaly(request);
        return ResponseEntity.ok(ApiResponse.success("시나리오 4 (패널 오염 감지)가 성공적으로 트리거되었습니다."));
    }


    // --- 시간 제어 및 상태 조회 API ---

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

    // --- 기존 트리거 API (Swagger UI에서 숨김) ---
    @Hidden
    @PostMapping("/trigger-power-anomaly")
    public void triggerPowerAnomaly(@RequestBody PowerAnomalyTriggerRequest request) {
        simulationService.triggerPowerAnomaly(request);
    }

    @Hidden
    @PostMapping("/trigger-vision-anomaly")
    public void triggerVisionAnomaly(@RequestBody VisionAnomalyTriggerRequest request) {
        simulationService.triggerVisionAnomaly(request);
    }
}
