package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.ForecastExplanationDto;
import com.solarwise.capstonebackend.dto.ForecastResponseDto;
import com.solarwise.capstonebackend.event.ForecastGenerationEvent;
import com.solarwise.capstonebackend.service.ForecastService;
import com.solarwise.capstonebackend.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "Forecasts", description = "발전량 예측 API")
@RestController
@RequestMapping("/api/v1/plants/{plantId}/forecasts")
@RequiredArgsConstructor
@Slf4j
public class ForecastController {

    private final ForecastService forecastService;
    private final ApplicationEventPublisher eventPublisher;
    private final SimulationService simulationService; // SimulationService 주입

    @Operation(summary = "AI 기반 예측 데이터 생성 요청 (수동)", description = "현재 가상 시간을 기준으로 AI에게 예측 데이터 생성을 요청하는 이벤트를 발행합니다.")
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<String>> generateForecasts(@PathVariable Long plantId) {
        // 💡 요구사항 해결: 현재 가상 시간을 기준으로 이벤트 발행
        LocalDateTime forecastTime = simulationService.getVirtualCurrentTime();
        eventPublisher.publishEvent(new ForecastGenerationEvent(this, plantId, forecastTime));
        return ResponseEntity.ok(ApiResponse.success("AI 예측 데이터 생성 요청이 전달되었습니다."));
    }

    @Operation(summary = "저장된 예측 발전량 조회", description = "DB에 저장된 예측 발전량 데이터를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<ForecastResponseDto>> getForecasts(
            @PathVariable Long plantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        ForecastResponseDto response = forecastService.getStoredForecasts(plantId, from, to);
        return ResponseEntity.ok(ApiResponse.success(response, "예측 데이터 조회 성공"));
    }

    @Operation(summary = "예측 설명 조회", description = "특정 예측 시점에 대한 AI의 원인 분석 설명을 조회합니다.")
    @GetMapping("/explanations")
    public ResponseEntity<ApiResponse<ForecastExplanationDto>> getForecastExplanations(
            @PathVariable Long plantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime targetTime) {
        log.info("GET /forecasts/explanations API 진입: plantId={}, targetTime={}", plantId, targetTime);
        ForecastExplanationDto response = forecastService.getForecastExplanations(plantId, targetTime);
        return ResponseEntity.ok(ApiResponse.success(response, "예측 설명 조회 성공"));
    }
}
