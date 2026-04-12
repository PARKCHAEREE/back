package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이미지 기반 이상 탐지 응답 DTO
 * - 패널 영상 분석 결과 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisionAnomalyDetectionResponse {

    @JsonProperty("panel_id")
    private String panelId; // 패널 ID

    @JsonProperty("plant_id")
    private String plantId; // 발전소 ID

    @JsonProperty("is_defective")
    private Boolean isDefective; // 결함 여부

    @JsonProperty("defect_type")
    private String defectType; // 결함 유형 (normal, dust, snow, bird_dropping, physical_damage)

    @JsonProperty("confidence")
    private Double confidence; // 신뢰도 (0.0 ~ 1.0)

    @JsonProperty("severity")
    private String severity; // 심각도 (LOW, MEDIUM, HIGH)

    @JsonProperty("recommendation")
    private String recommendation; // 권장 조치

}

