package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.DashboardResponse;
import com.solarwise.capstonebackend.repository.EnergyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 에너지 집계 서비스
 * - 시계열 데이터를 시간별/일별로 집계
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnergyAggregationService {

    private final EnergyLogRepository energyLogRepository;

    /**
     * 시간별 또는 일별로 집계된 에너지 데이터 조회
     */
    public List<DashboardResponse.AggregatedEnergyData> getAggregatedEnergyData(
            Long powerPlantId, LocalDateTime startTime, LocalDateTime endTime) {
        // TODO: 실제 집계 로직 구현
        return new ArrayList<>();
    }

}

