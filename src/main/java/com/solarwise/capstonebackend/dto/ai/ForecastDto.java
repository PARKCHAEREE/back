package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
public class ForecastDto {

    // AI 서버의 실제 응답은 'prediction' 키 아래에 중첩된 값을 포함할 수 있음
    // 혹은 targetTime, modelVersion 등이 상위 레벨에 있을 수 있음
    // 로그를 기반으로 가장 유연하게 처리
    
    @JsonProperty("target_time")
    private LocalDateTime targetTime;

    @JsonProperty("model_version")
    private String modelVersion;

    // 'prediction' 키가 객체일 경우를 대비
    private Map<String, Double> prediction;

    // 'prediction' 키가 단일 값일 경우를 대비
    @JsonProperty("prediction")
    private Double predictedPowerKw;

    private Double confidence;
}
