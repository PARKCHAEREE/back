package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.DashboardResponse;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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

    // 패널 히트맵 기능은 제거됨 (PanelHeatmapDto 삭제에 따라 관련 로직 정리)

}
