package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.DashboardSummaryDto;
import com.solarwise.capstonebackend.dto.DashboardTimelineResponse;
import com.solarwise.capstonebackend.dto.GapDto;
import com.solarwise.capstonebackend.dto.MeasurementDto;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.Forecast;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.ForecastRepository;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final PowerPlantRepository powerPlantRepository;
    private final PlantFeatureLogRepository plantFeatureLogRepository;
    private final ForecastRepository forecastRepository;
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

        LocalDateTime now = simulationService.getVirtualCurrentTime();
        LocalDateTime requestEnd = (to != null) ? to : now;
        
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

        List<PlantFeatureLog> actualLogs = plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(plantId, start, end);
        List<MeasurementDto> actualSeries = actualLogs.stream()
                .map(log -> MeasurementDto.builder()
                        .measuredAt(log.getMeasuredAt())
                        .powerKw(log.getActual())
                        .temperature(log.getTemp())
                        .irradiance(log.getIrradiance())
                        .humidity(log.getHumi())
                        .build())
                .collect(Collectors.toList());

        LocalDateTime futureEnd = end.plusHours(futureHours != null ? futureHours : 72);
        List<Forecast> forecastLogs = forecastRepository.findByPowerPlantIdAndTargetTimeBetween(plantId, start, futureEnd);
        List<MeasurementDto> predictionSeries = forecastLogs.stream()
                .map(log -> MeasurementDto.builder()
                        .measuredAt(log.getTargetTime())
                        .powerKw(log.getPredictedPowerKw())
                        .build())
                .collect(Collectors.toList());

        List<GapDto> gapSeries = actualLogs.stream()
                .map(log -> new GapDto(log.getMeasuredAt(), log.getPrediction(), log.getActual()))
                .collect(Collectors.toList());

        List<Anomaly> anomalyLogs = anomalyRepository.findByPowerPlantIdAndDetectedAtBetweenOrderByDetectedAtAsc(plantId, start, end);
        List<DashboardTimelineResponse.AnomalyMarker> anomalyMarkers = anomalyLogs.stream()
                .map(log -> DashboardTimelineResponse.AnomalyMarker.builder()
                        .eventId(log.getId())
                        .detectedAt(log.getDetectedAt())
                        .severity(log.getSeverity())
                        .summary(log.getSummary())
                        .build())
                .collect(Collectors.toList());

        return DashboardTimelineResponse.builder()
                .plantId(plantId)
                .lastUpdatedAt(simulationService.getVirtualCurrentTime())
                .actualSeries(actualSeries)
                .predictionSeries(predictionSeries)
                .gapSeries(gapSeries)
                .anomalyMarkers(anomalyMarkers)
                .build();
    }
}
