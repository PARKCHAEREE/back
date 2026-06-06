package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardSummaryDto {
    private Double currentPowerKw;
    private Double todayGenerationKwh;
    private Double todayForecastedKwh; // 💡 요구사항 반영: 금일 총 예측 발전량 필드 추가
    private Double efficiencyPercent;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime lastUpdatedAt;
    private AnomalyInfo latestAnomaly;

    @Data
    @Builder
    public static class AnomalyInfo {
        private boolean exists;
        private Long eventId;
        private String severity;
        private String summary;
    }
}
