package com.solarwise.capstonebackend.repository;

import com.solarwise.capstonebackend.entity.Anomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이상 탐지 저장소
 */
@Repository
public interface AnomalyRepository extends JpaRepository<Anomaly, Long> {

    List<Anomaly> findByPowerPlantIdOrderByDetectedAtDesc(Long powerPlantId);

    List<Anomaly> findByPowerPlantIdAndStatusOrderByDetectedAtDesc(Long powerPlantId, String status);

}

