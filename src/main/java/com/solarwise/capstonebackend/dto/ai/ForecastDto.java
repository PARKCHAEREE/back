package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastDto {

    // 💡 최종 수정: AI 서버의 실제 응답(String)을 그대로 받기 위해 타입 변경
    @JsonProperty("targetTime")
    private String targetTime;

    @JsonProperty("predictedPowerKw")
    private Double predictedPowerKw;

    @JsonProperty("confidence")
    private Double confidence;

    @JsonProperty("modelVersion")
    private String modelVersion;
}
