package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 서버의 발전량 예측 요청 DTO
 * - 히스토리 데이터 및 날씨 예보를 포함한 예측 요청
 * - 단일 예측 또는 시계열 예측 모두 지원
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPredictionRequest {

    @JsonProperty("plant_id")
    private String plantId; // 발전소 ID

    @JsonProperty("requested_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime requestedAt; // 요청 시각

    @JsonProperty("history")
    private List<HistoryDataDto> history; // 과거 데이터 (학습용)

    @JsonProperty("weather_forecast")
    private List<WeatherForecastDto> weatherForecast; // 날씨 예보 (단일 또는 시계열)

}



