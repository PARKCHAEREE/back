package com.solarwise.capstonebackend.repository;

import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 발전소 고급 피처 로그 저장소
 */
public interface PlantFeatureLogRepository extends JpaRepository<PlantFeatureLog, Long> {

    long countByPowerPlantId(Long powerPlantId);

    List<PlantFeatureLog> findByPowerPlantIdOrderByMeasuredAtDesc(Long powerPlantId, Pageable pageable);

    List<PlantFeatureLog> findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
            Long powerPlantId,
            LocalDateTime from,
            LocalDateTime to
    );
}

