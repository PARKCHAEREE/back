package com.solarwise.capstonebackend.repository;

import com.solarwise.capstonebackend.entity.Forecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 발전량 예측 저장소
 */
@Repository
public interface ForecastRepository extends JpaRepository<Forecast, Long> {

    /**
     * 특정 발전소의 예측 데이터를 시간 범위로 조회
     */
    List<Forecast> findByPowerPlantIdAndTargetTimeBetween(Long powerPlantId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 특정 발전소의 예측 데이터를 시간 오름차순으로 조회
     */
    List<Forecast> findByPowerPlantIdAndTargetTimeBetweenOrderByTargetTimeAsc(Long powerPlantId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 특정 발전소의 최신 예측 데이터 조회
     */
    List<Forecast> findByPowerPlantIdOrderByTargetTimeDesc(Long powerPlantId);

}

