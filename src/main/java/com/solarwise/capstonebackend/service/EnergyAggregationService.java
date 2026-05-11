package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.DashboardResponse;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 에너지 집계 서비스
 * - PlantFeatureLog 데이터를 시간별/일별로 집계
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnergyAggregationService {

    private final PlantFeatureLogRepository plantFeatureLogRepository;

    /**
     * 시간별 또는 일별로 집계된 에너지 데이터 조회
     * PlantFeatureLog의 actual 필드를 사용하여 발전량 집계
     */
    public List<DashboardResponse.AggregatedEnergyData> getAggregatedEnergyData(
            Long powerPlantId, LocalDateTime startTime, LocalDateTime endTime) {

        if (startTime == null || endTime == null) {
            log.debug("집계 시간 범위가 null이므로 빈 리스트 반환");
            return new ArrayList<>();
        }

        // 시간 범위 내의 PlantFeatureLog 데이터 조회
        List<com.solarwise.capstonebackend.entity.PlantFeatureLog> logs =
                plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
                        powerPlantId, startTime, endTime);

        if (logs.isEmpty()) {
            log.debug("집계할 데이터가 없음 - 발전소 ID: {}, 기간: {} ~ {}", powerPlantId, startTime, endTime);
            return new ArrayList<>();
        }

        // 시간별로 그룹화하여 집계 (1시간 단위)
        List<DashboardResponse.AggregatedEnergyData> aggregatedData = new ArrayList<>();
        LocalDateTime currentHour = startTime.truncatedTo(ChronoUnit.HOURS);

        while (currentHour.isBefore(endTime)) {
            final LocalDateTime finalCurrentHour = currentHour;
            LocalDateTime nextHour = currentHour.plusHours(1);

            // 현재 시간대의 로그 필터링
            List<com.solarwise.capstonebackend.entity.PlantFeatureLog> hourLogs = logs.stream()
                    .filter(log -> !log.getMeasuredAt().isBefore(finalCurrentHour) && log.getMeasuredAt().isBefore(nextHour))
                    .collect(Collectors.toList());

            if (!hourLogs.isEmpty()) {
                // 평균 발전량 계산 (actual 필드 사용)
                double avgPowerKw = hourLogs.stream()
                        .mapToDouble(log -> log.getActual() != null ? log.getActual() : 0.0)
                        .average()
                        .orElse(0.0);

                // 총 발전량 계산 (시간별 평균 * 1시간)
                double totalEnergyKwh = avgPowerKw * 1.0; // 1시간 단위

                // 평균 기상 데이터 계산
                double avgTemp = hourLogs.stream()
                        .mapToDouble(log -> log.getTemp() != null ? log.getTemp() : 0.0)
                        .average()
                        .orElse(0.0);

                double avgHumidity = hourLogs.stream()
                        .mapToDouble(log -> log.getHumi() != null ? log.getHumi() : 0.0)
                        .average()
                        .orElse(0.0);

                double avgIrradiance = hourLogs.stream()
                        .mapToDouble(log -> log.getIrradiance() != null ? log.getIrradiance() : 0.0)
                        .average()
                        .orElse(0.0);

                DashboardResponse.AggregatedEnergyData data = DashboardResponse.AggregatedEnergyData.builder()
                        .timestamp(currentHour)
                        .totalGeneration(totalEnergyKwh)
                        .avgPredictedGeneration(avgPowerKw)
                        .generationRate(0.0) // TODO: 실제 효율 계산
                        .build();

                aggregatedData.add(data);
            }

            currentHour = nextHour;
        }

        log.debug("에너지 데이터 집계 완료 - 발전소 ID: {}, 집계 건수: {}", powerPlantId, aggregatedData.size());
        return aggregatedData;
    }

}
