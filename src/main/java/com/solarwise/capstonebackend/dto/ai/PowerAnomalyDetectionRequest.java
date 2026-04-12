package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 전력 기반 이상 탐지 요청 DTO
 * - 발전량 및 기상 데이터를 기반으로 이상 탐지 요청
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerAnomalyDetectionRequest {

    @JsonProperty("plant_id")
    private String plantId; // 발전소 ID

    @JsonProperty("panel_id")
    private String panelId; // 패널 ID (optional)

    @JsonProperty("datetime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime datetime; // 측정 시간

    @JsonProperty("actual_power")
    private Double actualPower; // 실제 발전량 (kW)

    @JsonProperty("predicted_power")
    private Double predictedPower; // 예측 발전량 (kW)

    @JsonProperty("irradiation")
    private Double irradiation; // 일사량

    @JsonProperty("ambient_temperature")
    private Double ambientTemperature; // 주변 온도

    @JsonProperty("module_temperature")
    private Double moduleTemperature; // 모듈 온도

}

