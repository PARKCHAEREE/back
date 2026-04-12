package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 개별 예측 포인트 DTO
 * - 발전량 예측의 각 시점에 대한 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastDto {

    @JsonProperty("target_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime targetTime; // 예측 대상 시간

    @JsonProperty("predicted_power_kw")
    private Double predictedPowerKw; // 예측 발전량 (kW)

    @JsonProperty("confidence")
    private Double confidence; // 신뢰도 (0.0 ~ 1.0)

    @JsonProperty("model_version")
    private String modelVersion; // 사용된 모델 버전

    @JsonProperty("model_notes")
    private String modelNotes; // 모델 관련 메모

}

