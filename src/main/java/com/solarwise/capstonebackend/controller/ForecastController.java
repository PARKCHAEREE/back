package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ai.AiPredictionResponse;
import com.solarwise.capstonebackend.dto.ai.XaiExplanationResponse;
import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.service.AiIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@Tag(name = "Forecasts", description = "발전량 예측 및 XAI 관련 API")
@RestController
@RequestMapping("/api/v1/plants/{plantId}/forecasts")
@RequiredArgsConstructor
public class ForecastController {

    private final AiIntegrationService aiIntegrationService;
    @Operation(summary = "발전량 예측 조회")
    @GetMapping
    public ApiResponse<AiPredictionResponse> getForecast(@PathVariable Long plantId) {
        AiPredictionResponse prediction = aiIntegrationService.requestPredictionFromAi(plantId).join();
        return ApiResponse.success(prediction, "AI 예측 발전량 조회 성공");
    }
    @Operation(summary = "예측 근거(XAI) 조회")
    @GetMapping("/explanations")
    public ApiResponse<XaiExplanationResponse> getForecastExplanation(@PathVariable Long plantId) {
        XaiExplanationResponse explanation = aiIntegrationService.requestXaiExplanation(plantId).join();
        return ApiResponse.success(explanation,"XAI 예측 설명 조회 성공");
    }
}