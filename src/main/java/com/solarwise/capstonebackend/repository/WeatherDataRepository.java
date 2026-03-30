package com.solarwise.capstonebackend.repository;

import com.solarwise.capstonebackend.entity.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 기상 데이터 저장소
 */
@Repository
public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {

    List<WeatherData> findByPowerPlantIdAndTimestampBetween(Long powerPlantId, LocalDateTime start, LocalDateTime end);

}

