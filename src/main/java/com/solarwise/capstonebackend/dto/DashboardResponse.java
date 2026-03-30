package com.solarwise.capstonebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대시보드 응답 DTO
 * - 프론트엔드 차트 렌더링을 위한 집계 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private Long powerPlantId;
    private String plantName;
    private LocalDateTime dateRange; // 조회 시간 범위
    private List<AggregatedEnergyData> energyData; // 시간별/일별 집계 데이터
    private List<AnomalyDto> recentAnomalies; // 최근 이상 탐지

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AggregatedEnergyData {
        private LocalDateTime timestamp;
        private Double totalGeneration; // 누적 발전량
        private Double avgPredictedGeneration; // 평균 예측 발전량
        private Double generationRate; // 발전 효율
    }

}

