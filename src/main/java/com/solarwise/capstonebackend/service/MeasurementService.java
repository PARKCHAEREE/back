package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.DashboardSummaryDto;
import com.solarwise.capstonebackend.dto.MeasurementDto;
import com.solarwise.capstonebackend.dto.MeasurementSeriesDto;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.EnergyLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.EnergyLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 측정 데이터 서비스
 * - 시계열 발전량 데이터 조회 및 대시보드 요약
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeasurementService {

    private final EnergyLogRepository energyLogRepository;
    private final PowerPlantRepository powerPlantRepository;
    private final AnomalyRepository anomalyRepository;

    /**
     * 대시보드 요약 조회
     */
    public DashboardSummaryDto getDashboardSummary(Long plantId, Long userId) {
        PowerPlant plant = powerPlantRepository.findByIdAndUserId(plantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        // 현재 발전량 (가장 최근 데이터)
        EnergyLog latestLog = energyLogRepository.findTopByPowerPlantIdOrderByTimestampDesc(plantId)
                .orElse(null);

        Double currentPowerKw = latestLog != null ? latestLog.getPowerKw() : 0.0;

        // 금일 발전량 (오늘 00:00 ~ 23:59)
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        List<EnergyLog> todayLogs = energyLogRepository
                .findByPowerPlantIdAndTimestampBetween(plantId, todayStart, todayEnd);

        Double todayGenerationKwh = todayLogs.stream()
                .mapToDouble(EnergyLog::getPowerKw)
                .sum() / 60; // kW를 kWh로 변환 (시간 단위 집계 가정)

        // 효율 (향후 확장)
        Double efficiencyPercent = 87.1; // TODO: 실제 효율 계산

        // 최근 이상 정보
        List<Anomaly> recentAnomalies = anomalyRepository
                .findByPowerPlantIdOrderByDetectedAtDesc(plantId);

        DashboardSummaryDto.AnomalyInfo anomalyInfo = null;
        if (!recentAnomalies.isEmpty()) {
            Anomaly latest = recentAnomalies.get(0);
            anomalyInfo = DashboardSummaryDto.AnomalyInfo.builder()
                    .exists(true)
                    .eventId(latest.getId())
                    .severity(latest.getSeverity())
                    .summary(latest.getSummary())
                    .build();
        } else {
            anomalyInfo = DashboardSummaryDto.AnomalyInfo.builder()
                    .exists(false)
                    .build();
        }

        return DashboardSummaryDto.builder()
                .currentPowerKw(currentPowerKw)
                .todayGenerationKwh(todayGenerationKwh)
                .efficiencyPercent(efficiencyPercent)
                .lastUpdatedAt(latestLog != null ? latestLog.getTimestamp() : LocalDateTime.now())
                .latestAnomaly(anomalyInfo)
                .build();
    }

    /**
     * 시계열 측정 데이터 조회
     */
    public MeasurementSeriesDto getMeasurementSeries(Long plantId, Long userId,
                                                      LocalDateTime from, LocalDateTime to) {
        PowerPlant plant = powerPlantRepository.findByIdAndUserId(plantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        List<EnergyLog> measurements = energyLogRepository
                .findByPowerPlantIdAndTimestampBetween(plantId, from, to);

        List<MeasurementDto> series = measurements.stream()
                .map(log -> MeasurementDto.builder()
                        .measuredAt(log.getTimestamp())
                        .powerKw(log.getPowerKw())
                        .temperature(log.getTemperature())
                        .irradiance(log.getIrradiance())
                        .humidity(log.getHumidity())
                        .build())
                .collect(Collectors.toList());

        return MeasurementSeriesDto.builder()
                .plantId(plantId)
                .series(series)
                .build();
    }

}


