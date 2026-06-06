package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대시보드 타임라인 응답 DTO
 *
 * 구성:
 * - virtualNow: 현재 가상 시간 (슬라이딩 윈도우 계산 기준)
 * - windowStart/End: DB에 적재된 CSV 실측값 조회 범위
 * - forecastEnd: DB에 적재된 CSV 예측값 조회 범위 (현재 이후 미래 포함)
 * - actualSeries: plant_feature_logs.actual 값들 (현재 이하, 시간순)
 * - predictionSeries: plant_feature_logs.prediction 값들 (미래 포함, 시간순)
 * - gapSeries: 같은 CSV 행의 예측값과 실측값 차이 (이상 탐지 기준)
 * - anomalyMarkers: 감지된 이상 현상들
 *
 * 화면 모델:
 * 예측량은 현재를 지나 미래까지 먼저 표시되고, 발전량은 시간이 흐르며
 * 그 예측 궤적을 따라오는지 확인하는 방식으로 렌더링된다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTimelineResponse {

    private Long plantId;

    private String range;  // DAY, WEEK, MONTH

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime virtualNow;  // 현재 가상 시간

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime windowStart;  // 조회 범위 시작 (과거)

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime windowEnd;    // 조회 범위 종료 (현재)

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime forecastEnd;  // 예측 범위 종료 (미래)

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime lastUpdatedAt;

    private List<MeasurementDto> actualSeries;      // [windowStart, windowEnd] CSV actual 값
    private List<MeasurementDto> predictionSeries;  // [windowStart, forecastEnd] CSV prediction 값
    private List<GapDto> gapSeries;                 // 같은 시간대 prediction/actual 차이
    private List<AnomalyMarker> anomalyMarkers;     // 감지된 이상

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnomalyMarker {
        private Long eventId;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime detectedAt;

        private String type;        // POWER, VISION
        private String severity;    // LOW, MEDIUM, HIGH
        private String status;      // OPEN, ACKNOWLEDGED, RESOLVED
        private String summary;     // 사람이 읽을 수 있는 요약
    }
}
