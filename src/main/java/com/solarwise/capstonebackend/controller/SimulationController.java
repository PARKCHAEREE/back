package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 시뮬레이션 컨트롤러
 * - 가상 시간 기반 시뮬레이션 제어 API를 제공합니다.
 */
@Tag(name = "Simulation API", description = "가상 시간 기반 시뮬레이션 제어")
@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    /**
     * 현재 가상 시간을 조회합니다.
     *
     * @return 가상 현재 시간
     */
    @Operation(summary = "가상 현재 시간 조회")
    @GetMapping("/time")
    public ResponseEntity<ApiResponse<LocalDateTime>> getVirtualTime() {
        LocalDateTime currentTime = simulationService.getVirtualCurrentTime();
        return ResponseEntity.ok(ApiResponse.success(currentTime, "가상 현재 시간 조회 성공"));
    }

    /**
     * 가상 시간을 1시간 앞으로 땡깁니다.
     *
     * @return 업데이트된 가상 시간
     */
    @Operation(summary = "가상 시간 1시간 전진")
    @PostMapping("/tick")
    public ResponseEntity<ApiResponse<LocalDateTime>> advanceTime() {
        simulationService.advanceTimeByHour();
        LocalDateTime updatedTime = simulationService.getVirtualCurrentTime();
        return ResponseEntity.ok(ApiResponse.success(updatedTime, "가상 시간 1시간 전진 완료"));
    }

    /**
     * 드론 에러 트리거를 설정합니다.
     *
     * @return 트리거 설정 결과
     */
    @Operation(summary = "드론 에러 트리거 설정")
    @PostMapping("/trigger-drone-error")
    public ResponseEntity<ApiResponse<String>> triggerDroneError() {
        simulationService.triggerDroneError();
        return ResponseEntity.ok(ApiResponse.success("드론 에러 트리거 설정됨", "다음 스케줄 실행 시 파손 패널 이미지 분석이 수행됩니다."));
    }
}
