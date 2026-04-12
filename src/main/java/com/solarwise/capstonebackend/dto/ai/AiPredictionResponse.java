package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import com.solarwise.capstonebackend.dto.ai.ForecastDto;
import com.solarwise.capstonebackend.dto.ai.XaiExplanationDto;

/**
 * AI 서버의 발전량 예측 응답 DTO
 * - 시계열 예측 결과 및 설명 가능한 AI(XAI) 정보 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPredictionResponse {

    @JsonProperty("plant_id")
    private String plantId; // 발전소 ID

    @JsonProperty("forecast_series")
    private List<ForecastDto> forecastSeries; // 시계열 예측 결과

    @JsonProperty("explanations")
    private List<XaiExplanationDto> explanations; // 설명 가능한 AI 정보

    @JsonProperty("drift_detected")
    private Boolean driftDetected; // 데이터 드리프트 감지 여부

}


