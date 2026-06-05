package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.service.AiIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Tag(name = "AI 테스트 API", description = "AI 서버 연동 및 XAI 기능 분리 테스트")
@RestController
@RequestMapping("/api/test/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final AiIntegrationService aiIntegrationService;

    @Operation(summary = "1. [대시보드] 테스트", description = "AI 서버에 대시보드 데이터 조회를 요청합니다.")
    @PostMapping("/dashboard/{plantId}")
    public ResponseEntity<ApiResponse<Map>> testDashboard(@PathVariable Long plantId) throws ExecutionException, InterruptedException {
        Map result = aiIntegrationService.requestDashboard(plantId, "crack.jpg").get();
        return ResponseEntity.ok(ApiResponse.success(result, "AI 대시보드 조회 성공"));
    }

    @Operation(summary = "2. [이상탐지 XAI] 테스트", description = "AI 서버에 이상탐지 XAI 분석을 요청합니다.")
    @PostMapping("/anomaly-xai/{plantId}")
    public ResponseEntity<ApiResponse<Map>> testAnomalyXai(@PathVariable Long plantId) throws ExecutionException, InterruptedException {
        Map result = aiIntegrationService.requestAnomalyXai(plantId, "crack.jpg").get();
        return ResponseEntity.ok(ApiResponse.success(result, "AI 이상탐지 XAI 분석 성공"));
    }

    @Operation(summary = "3. [이상탐지 상세] 테스트", description = "AI 서버에 이상탐지 상세 분석을 요청합니다.")
    @PostMapping("/anomaly-detail/{plantId}")
    public ResponseEntity<ApiResponse<Map>> testAnomalyDetail(@PathVariable Long plantId) throws ExecutionException, InterruptedException {
        Map result = aiIntegrationService.requestAnomalyDetail(plantId, "crack.jpg").get();
        return ResponseEntity.ok(ApiResponse.success(result, "AI 이상탐지 상세 분석 성공"));
    }

    @Operation(summary = "4. [발전량 예측] 테스트", description = "AI 서버에 발전량 예측을 요청합니다.")
    @PostMapping("/power-forecast/{plantId}")
    public ResponseEntity<ApiResponse<Map>> testPowerForecast(@PathVariable Long plantId) throws ExecutionException, InterruptedException {
        Map result = aiIntegrationService.requestPowerForecast(plantId).get();
        return ResponseEntity.ok(ApiResponse.success(result, "AI 발전량 예측 성공"));
    }
}
