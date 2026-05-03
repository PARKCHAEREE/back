package com.solarwise.capstonebackend.repository;

import com.solarwise.capstonebackend.entity.EnergyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 에너지 로그 저장소
 */
@Repository
public interface EnergyLogRepository extends JpaRepository<EnergyLog, Long> {

    List<EnergyLog> findByPowerPlantIdAndTimestampBetween(Long powerPlantId, LocalDateTime start, LocalDateTime end);

    List<EnergyLog> findByPowerPlantIdAndTimestampBetweenOrderByTimestampAsc(Long powerPlantId, LocalDateTime start, LocalDateTime end);

    Optional<EnergyLog> findTopByPowerPlantIdOrderByTimestampDesc(Long powerPlantId);

}

