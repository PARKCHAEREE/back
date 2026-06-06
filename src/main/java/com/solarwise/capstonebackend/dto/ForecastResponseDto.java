package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ForecastResponseDto {
    private Long plantId;
    private LocalDateTime generatedAt;

    @JsonProperty("forecast_series") // 💡 프론트엔드 에러 해결
    private List<ForecastItem> series;

    @Data
    @Builder
    public static class ForecastItem {
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime targetTime;
        private Double predictedPowerKw;
        private Double confidence;
    }
}
