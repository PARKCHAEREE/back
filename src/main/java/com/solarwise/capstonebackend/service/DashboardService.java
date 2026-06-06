package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.DashboardSummaryDto;
import com.solarwise.capstonebackend.dto.DashboardTimelineResponse;
import com.solarwise.capstonebackend.dto.GapDto;
import com.solarwise.capstonebackend.dto.MeasurementDto;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final PowerPlantRepository powerPlantRepository;
    private final PlantFeatureLogRepository plantFeatureLogRepository;
    private final AnomalyRepository anomalyRepository;
    private final SimulationService simulationService;

    public DashboardSummaryDto getDashboardSummary(Long plantId, Long userId) {
        powerPlantRepository.findByIdAndUserId(plantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        
        LocalDateTime todayStart = virtualNow.toLocalDate().atStartOfDay();
        List<PlantFeatureLog> todayLogs = plantFeatureLogRepository
                .findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(plantId, todayStart, virtualNow);

        // 💡 처방 3: '현재 발전량'을 오늘 데이터 중 가장 마지막 값으로 계산
        Double currentPowerKw = todayLogs.isEmpty() ? 0.0 : 
            todayLogs.get(todayLogs.size() - 1).getActual();
        currentPowerKw = (currentPowerKw == null) ? 0.0 : currentPowerKw;

        double todayGenerationKwh = todayLogs.stream()
                .mapToDouble(log -> log.getActual() != null ? log.getActual() : 0.0)
                .sum();
        double todayForecastedKwh = todayLogs.stream()
                .mapToDouble(log -> log.getPrediction() != null ? log.getPrediction() : 0.0)
                .sum();

        double efficiencyPercent = 0.0;
        if (todayForecastedKwh > 0) {
            efficiencyPercent = (todayGenerationKwh / todayForecastedKwh) * 100;
        }

        Anomaly latestAnomaly = anomalyRepository.findByPowerPlantIdOrderByDetectedAtDesc(plantId).stream().findFirst().orElse(null);
        DashboardSummaryDto.AnomalyInfo anomalyInfo = (latestAnomaly != null) ?
                DashboardSummaryDto.AnomalyInfo.builder()
                        .exists(true)
                        .eventId(latestAnomaly.getId())
                        .severity(latestAnomaly.getSeverity())
                        .summary(latestAnomaly.getSummary())
                        .build() :
                DashboardSummaryDto.AnomalyInfo.builder().exists(false).build();

        return DashboardSummaryDto.builder()
                .currentPowerKw(currentPowerKw)
                .todayGenerationKwh(todayGenerationKwh)
                .todayForecastedKwh(todayForecastedKwh)
                .efficiencyPercent(efficiencyPercent)
                .lastUpdatedAt(virtualNow)
                .latestAnomaly(anomalyInfo)
                .build();
    }

    public DashboardTimelineResponse getDashboardTimeline(Long plantId, Long userId, String range, Integer futureHours, LocalDateTime to) {
        powerPlantRepository.findByIdAndUserId(plantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        PlantFeatureLog earliest = plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtAsc(plantId);
        PlantFeatureLog latest = plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtDesc(plantId);
        if (earliest == null || latest == null) {
            return DashboardTimelineResponse.builder()
                    .plantId(plantId)
                    .range(range.toUpperCase())
                    .virtualNow(simulationService.getVirtualCurrentTime())
                    .windowStart(null)
                    .windowEnd(null)
                    .forecastEnd(null)
                    .lastUpdatedAt(simulationService.getVirtualCurrentTime())
                    .actualSeries(List.of())
                    .predictionSeries(List.of())
                    .gapSeries(List.of())
                    .anomalyMarkers(List.of())
                    .build();
        }

        LocalDateTime now = simulationService.getVirtualCurrentTime();
        LocalDateTime requestedTime = (to != null) ? to : now;
        LocalDateTime requestEnd = normalizeToDataRange(requestedTime, earliest.getMeasuredAt(), latest.getMeasuredAt());
        
        LocalDateTime start;
        LocalDateTime end;

        switch (range.toUpperCase()) {
            case "HOUR":
                start = requestEnd.minusHours(1);
                end = requestEnd;
                break;
            case "WEEK":
                start = requestEnd.minusWeeks(1);
                end = requestEnd;
                break;
            case "MONTH":
                start = requestEnd.minusMonths(1);
                end = requestEnd;
                break;
            case "DAY":
            default:
                start = requestEnd.minusDays(1);
                end = requestEnd;
                break;
        }

        if (start.isBefore(earliest.getMeasuredAt())) {
            start = earliest.getMeasuredAt();
        }
        if (end.isAfter(latest.getMeasuredAt())) {
            end = latest.getMeasuredAt();
        }

        // 실측 데이터: DB에 적재된 CSV 행의 actual 컬럼을 현재 이하 구간만 조회한다.
        List<PlantFeatureLog> actualLogs = plantFeatureLogRepository
                .findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(plantId, start, end);
        if (actualLogs.isEmpty()) {
            LocalDateTime anchorTime = resolveAnchorTime(plantId, end);
            if (anchorTime != null) {
                end = anchorTime;
                switch (range.toUpperCase()) {
                    case "HOUR":
                        start = end.minusHours(1);
                        break;
                    case "WEEK":
                        start = end.minusWeeks(1);
                        break;
                    case "MONTH":
                        start = end.minusMonths(1);
                        break;
                    case "DAY":
                    default:
                        start = end.minusDays(1);
                        break;
                }
                actualLogs = plantFeatureLogRepository
                        .findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(plantId, start, end);
                log.warn("Timeline window adjusted to latest data range for plantId={} (start={}, end={})", plantId, start, end);
            }
        }

        List<MeasurementDto> actualSeries = actualLogs.stream()
                .map(log -> MeasurementDto.builder()
                        .measuredAt(log.getMeasuredAt())
                        .powerKw(log.getActual())
                        .temperature(log.getTemp())
                        .irradiance(log.getIrradiance())
                        .humidity(log.getHumi())
                        .build())
                .collect(Collectors.toList());

        // 예측 데이터: 같은 CSV 기반 테이블의 prediction 컬럼을 미래 구간까지 조회한다.
        // 대시보드는 이 값을 먼저 깔아두고, actualSeries가 시간이 흐르며 그 궤적을 따라오는지 보여준다.
        LocalDateTime forecastEnd = end.plusHours(futureHours != null ? futureHours : 72);
        if (forecastEnd.isAfter(latest.getMeasuredAt())) {
            forecastEnd = latest.getMeasuredAt();
        }
        List<PlantFeatureLog> predictionLogs = plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(plantId, start, forecastEnd);
        List<MeasurementDto> predictionSeries = predictionLogs.stream()
                .map(log -> MeasurementDto.builder()
                        .measuredAt(log.getMeasuredAt())
                        .powerKw(log.getPrediction())
                        .build())
                .collect(Collectors.toList());

        // 차이 계산: 현재 이하 구간에서 같은 CSV 행의 prediction과 actual을 비교한다.
        List<GapDto> gapSeries = actualLogs.stream()
                .map(log -> new GapDto(log.getMeasuredAt(), log.getPrediction(), log.getActual()))
                .collect(Collectors.toList());

        // 이상 탐지 마커: [start, end] 범위
        List<Anomaly> anomalyLogs = anomalyRepository.findByPowerPlantIdAndDetectedAtBetweenOrderByDetectedAtAsc(plantId, start, end);
        List<DashboardTimelineResponse.AnomalyMarker> anomalyMarkers = anomalyLogs.stream()
                .map(log -> DashboardTimelineResponse.AnomalyMarker.builder()
                        .eventId(log.getId())
                        .detectedAt(log.getDetectedAt())
                        .type(log.getType())
                        .severity(log.getSeverity())
                        .status(log.getStatus())
                        .summary(log.getSummary())
                        .build())
                .collect(Collectors.toList());

        return DashboardTimelineResponse.builder()
                .plantId(plantId)
                .range(range.toUpperCase())
                .virtualNow(now)
                .windowStart(start)
                .windowEnd(end)
                .forecastEnd(forecastEnd)
                .lastUpdatedAt(now)
                .actualSeries(actualSeries)
                .predictionSeries(predictionSeries)
                .gapSeries(gapSeries)
                .anomalyMarkers(anomalyMarkers)
                .build();
    }

    private LocalDateTime resolveAnchorTime(Long plantId, LocalDateTime requestEnd) {
        List<PlantFeatureLog> nearestBeforeOrEqual = plantFeatureLogRepository
                .findByPowerPlantIdAndMeasuredAtLessThanEqualOrderByMeasuredAtDesc(
                        plantId,
                        requestEnd,
                        PageRequest.of(0, 1)
                );
        if (!nearestBeforeOrEqual.isEmpty()) {
            return nearestBeforeOrEqual.get(0).getMeasuredAt();
        }

        PlantFeatureLog latestAny = plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtDesc(plantId);
        return latestAny != null ? latestAny.getMeasuredAt() : null;
    }

    private LocalDateTime normalizeToDataRange(LocalDateTime target, LocalDateTime dataStart, LocalDateTime dataEnd) {
        if (target == null || dataStart == null || dataEnd == null || dataStart.isAfter(dataEnd)) {
            return target;
        }
        if (!target.isBefore(dataStart) && !target.isAfter(dataEnd)) {
            return target;
        }

        long periodHours = ChronoUnit.HOURS.between(dataStart, dataEnd) + 1;
        if (periodHours <= 0) {
            return dataStart;
        }

        long offsetHours = ChronoUnit.HOURS.between(dataStart, target);
        long normalizedOffset = Math.floorMod(offsetHours, periodHours);
        LocalDateTime normalized = dataStart.plusHours(normalizedOffset);
        log.debug("Timeline virtual time normalized: requested={}, normalized={}, range=[{}, {}]",
                target, normalized, dataStart, dataEnd);
        return normalized;
    }
}
