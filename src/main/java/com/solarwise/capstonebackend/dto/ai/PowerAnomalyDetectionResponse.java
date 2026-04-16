package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 전력 기반 이상 탐지 응답 DTO
 * - 이상 탐지 결과 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerAnomalyDetectionResponse {

    @JsonProperty("plant_id")
    private String plantId; // 발전소 ID

    @JsonProperty("panel_id")
    private String panelId; // 패널 ID

    @JsonProperty("actual_power")
    private Double actualPower; // 실제 발전량 (kW)

    @JsonProperty("predicted_power")
    private Double predictedPower; // 예측 발전량 (kW)

    @JsonProperty("is_anomaly")
    private Boolean isAnomaly; // 이상 여부

    @JsonProperty("anomaly_score")
    private Double anomalyScore; // 이상 점수 (0.0 ~ 1.0)

    @JsonProperty("severity")
    private String severity; // 심각도 (LOW, MEDIUM, HIGH)

    @JsonProperty("recommendation")
    private String recommendation; // 권장 조치

}


