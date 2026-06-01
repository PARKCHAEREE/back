package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.AnomalyMarkerDto;
import com.solarwise.capstonebackend.dto.DashboardResponse;
import com.solarwise.capstonebackend.dto.DashboardTimelineResponse;
import com.solarwise.capstonebackend.dto.GapPointDto;
import com.solarwise.capstonebackend.dto.TimePointDto;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * 대시보드 서비스
 * - 프론트엔드 차트 데이터 집계
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PowerPlantRepository powerPlantRepository;
    private final EnergyAggregationService energyAggregationService;
    private final AnomalyService anomalyService;
    private final SimulationService simulationService;
    private final AnomalyRepository anomalyRepository;
    private final PlantFeatureLogRepository plantFeatureLogRepository;

    // 인메모리 캐시를 사용하여 초당 반복 호출 시 DB 부하를 줄입니다.
    // 주의: 이 캐시 로직은 시연용으로 단순화되어 있으며, 실제 운영 시에는
    // 캐시 무효화 전략과 분산 캐시 고려가 필요합니다. 이 부분은 팀원과 스키마 변경 공유가 필요합니다.
    private final java.util.concurrent.ConcurrentHashMap<Long, DashboardTimelineResponse> timelineCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<Long, java.time.LocalDateTime> timelineCacheVirtualNow = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 발전소 대시보드 조회
     */
    public DashboardResponse getDashboard(Long powerPlantId, LocalDateTime startTime, LocalDateTime endTime) {
        PowerPlant plant = powerPlantRepository.findById(powerPlantId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        // ✅ FIXED: 가상 시간 기준으로 조회 (파라미터가 없으면 가상 현재 시간 사용)
        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        LocalDateTime actualEndTime = endTime != null ? endTime : virtualNow;

        return DashboardResponse.builder()
                .powerPlantId(powerPlantId)
                .plantName(plant.getName())
                .dateRange(actualEndTime)
                .energyData(energyAggregationService.getAggregatedEnergyData(powerPlantId, startTime, actualEndTime))
                .recentAnomalies(anomalyService.getRecentAnomalies(powerPlantId, 10))
                .build();
    }

    /**
     * 대시보드 타임라인 통합 조회
     * - 실측(과거~현재), 예측(과거~미래), 괴리, 이상 마커를 한 번에 제공합니다.
     */
    public DashboardTimelineResponse getDashboardTimeline(
            Long powerPlantId,
            Long userId,
            String range,
            Integer futureHours,
            LocalDateTime to
    ) {
        powerPlantRepository.findByIdAndUserId(powerPlantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        LocalDateTime windowEnd = to != null ? to : virtualNow;

        TimelineRange timelineRange = TimelineRange.from(range);
        LocalDateTime windowStart = windowEnd.minusHours(timelineRange.windowHours());

        int resolvedFutureHours = futureHours != null
                ? Math.max(0, Math.min(futureHours, 24 * 14))
                : timelineRange.defaultFutureHours();
        LocalDateTime forecastEnd = windowEnd.plusHours(resolvedFutureHours);

        // 캐시 체크: 시뮬레이션의 마지막 tick 시점(lastTickAt)을 기준으로 캐시 갱신 여부 판단
        java.time.LocalDateTime lastTick = simulationService.getLastTickAt();
        DashboardTimelineResponse cached = timelineCache.get(powerPlantId);
        java.time.LocalDateTime cachedVirtualNow = timelineCacheVirtualNow.get(powerPlantId);

        if (cached != null && cachedVirtualNow != null && lastTick != null && !lastTick.isAfter(cachedVirtualNow)) {
            // 시뮬레이션이 전진하지 않았다면 캐시된 응답 반환
            return cached;
        }

        // 실제 DB 조회 (초기 1회 또는 시뮬레이션이 전진했을 때만 수행)
        List<PlantFeatureLog> actualLogs = plantFeatureLogRepository
                .findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(powerPlantId, windowStart, windowEnd);
        List<PlantFeatureLog> predictionLogs = plantFeatureLogRepository
                .findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(powerPlantId, windowStart, forecastEnd);
        List<Anomaly> anomalies = anomalyRepository
                .findByPowerPlantIdAndDetectedAtBetweenOrderByDetectedAtAsc(powerPlantId, windowStart, windowEnd);

        List<TimePointDto> actualSeries = actualLogs.stream()
                .filter(log -> log.getActual() != null)
                .map(log -> TimePointDto.builder()
                        .ts(log.getMeasuredAt())
                        .value(log.getActual())
                        .build())
                .toList();

        List<TimePointDto> predictionSeries = predictionLogs.stream()
                .filter(log -> log.getPrediction() != null)
                .map(log -> TimePointDto.builder()
                        .ts(log.getMeasuredAt())
                        .value(log.getPrediction())
                        .build())
                .toList();

        List<GapPointDto> gapSeries = actualLogs.stream()
                .filter(log -> log.getActual() != null && log.getPrediction() != null)
                .map(this::toGapPoint)
                .toList();

        List<AnomalyMarkerDto> anomalyMarkers = anomalies.stream()
                .filter(anomaly -> anomaly.getDetectedAt() != null)
                .map(anomaly -> AnomalyMarkerDto.builder()
                        .eventId(anomaly.getId())
                        .ts(anomaly.getDetectedAt())
                        .type(anomaly.getType())
                        .severity(anomaly.getSeverity())
                        .status(anomaly.getStatus())
                        .summary(anomaly.getSummary())
                        .build())
                .toList();

        DashboardTimelineResponse response = DashboardTimelineResponse.builder()
                .plantId(powerPlantId)
                .range(timelineRange.name())
                .virtualNow(virtualNow)
                .windowStart(windowStart)
                .windowEnd(windowEnd)
                .forecastEnd(forecastEnd)
                .actualSeries(actualSeries)
                .predictionSeries(predictionSeries)
                .gapSeries(gapSeries)
                .anomalyMarkers(anomalyMarkers)
                .build();

        // 캐시에 저장
        timelineCache.put(powerPlantId, response);
        timelineCacheVirtualNow.put(powerPlantId, lastTick != null ? lastTick : virtualNow);

        return response;
    }

    private GapPointDto toGapPoint(PlantFeatureLog log) {
        double actual = log.getActual();
        double prediction = log.getPrediction();
        double absGap = Math.abs(actual - prediction);
        double denominator = Math.max(Math.abs(prediction), 1e-9);
        double gapRate = absGap / denominator;

        return GapPointDto.builder()
                .ts(log.getMeasuredAt())
                .absGap(absGap)
                .gapRate(gapRate)
                .build();
    }

    private enum TimelineRange {
        DAY(24, 24),
        WEEK(24 * 7, 48),
        MONTH(24 * 30, 72);

        private final int windowHours;
        private final int defaultFutureHours;

        TimelineRange(int windowHours, int defaultFutureHours) {
            this.windowHours = windowHours;
            this.defaultFutureHours = defaultFutureHours;
        }

        public int windowHours() {
            return windowHours;
        }

        public int defaultFutureHours() {
            return defaultFutureHours;
        }

        public static TimelineRange from(String value) {
            if (value == null || value.isBlank()) {
                return DAY;
            }
            try {
                return TimelineRange.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return DAY;
            }
        }
    }

    // 패널 히트맵 기능은 제거됨 (PanelHeatmapDto 삭제에 따라 관련 로직 정리)

}
