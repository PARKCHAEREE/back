package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대시보드 요약 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {

    private Double currentPowerKw; // 현재 발전 전력 (kW)

    private Double todayGenerationKwh; // 금일 발전량 (kWh)

    private Double efficiencyPercent; // 효율 (%)

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime lastUpdatedAt; // 마지막 업데이트 시각

    private AnomalyInfo latestAnomaly; // 최근 이상 정보

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnomalyInfo {
        private boolean exists; // 이상 여부
        private Long eventId; // 이상 이벤트 ID
        private String severity; // 심각도
        private String summary; // 요약
    }

}

