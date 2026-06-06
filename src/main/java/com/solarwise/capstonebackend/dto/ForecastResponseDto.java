package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ForecastResponseDto {
    private Long plantId;
    private LocalDateTime generatedAt;

    // 💡 최종 수정: 명세서(6-1)에 따라, JSON 키가 "series"가 되도록 @JsonProperty 제거
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
