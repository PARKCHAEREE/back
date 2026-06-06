package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.AnomalyDto;
import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.service.AiIntegrationService;
import com.solarwise.capstonebackend.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final SimulationService simulationService;

    @Value("${ai.server.base-url}")
    private String aiServerBaseUrl;

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

    @Operation(summary = "3. [이상탐지 상세] 테스트 (Heatmap URL 조립)", description = "AI 서버의 응답에서 heatmap_url을 절대 경로로 조립하여 반환합니다.")
    @PostMapping("/anomaly-detail/{plantId}")
    public ResponseEntity<ApiResponse<AnomalyDto>> testAnomalyDetail(@PathVariable Long plantId) throws ExecutionException, InterruptedException {
        Map<String, Object> aiResult = aiIntegrationService.requestAnomalyDetail(plantId, "crack.jpg").get();

        String relativeUrl = (String) aiResult.get("heatmap_url");
        String absoluteHeatmapUrl = null;
        if (relativeUrl != null && !relativeUrl.isBlank()) {
            absoluteHeatmapUrl = aiServerBaseUrl + relativeUrl;
        }

        AnomalyDto responseDto = AnomalyDto.builder()
                .cause((String) aiResult.get("cause"))
                .severity((String) aiResult.get("severity"))
                .recommendedAction((String) aiResult.get("recommendation"))
                .heatmapUrl(absoluteHeatmapUrl)
                .build();

        return ResponseEntity.ok(ApiResponse.success(responseDto, "AI 이상탐지 상세 분석 성공"));
    }

    @Operation(summary = "4. [발전량 예측] 테스트", description = "AI 서버에 발전량 예측을 요청합니다.")
    @PostMapping("/power-forecast/{plantId}")
    public ResponseEntity<ApiResponse<Map>> testPowerForecast(@PathVariable Long plantId) throws ExecutionException, InterruptedException {
        // 💡 오류 해결: 두 번째 인자로 현재 가상 시간을 전달
        Map result = aiIntegrationService.requestPowerForecast(plantId, simulationService.getVirtualCurrentTime()).get();
        return ResponseEntity.ok(ApiResponse.success(result, "AI 발전량 예측 성공"));
    }
}
