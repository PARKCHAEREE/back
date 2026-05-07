package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.PlantFeatureLogDto;
import com.solarwise.capstonebackend.dto.PlantFeatureLogSeriesDto;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 발전소 고급 피처 로그 서비스
 */
@Service
@RequiredArgsConstructor
public class PlantFeatureLogService {

    private final PlantFeatureLogRepository plantFeatureLogRepository;
    private final PowerPlantRepository powerPlantRepository;

    public long getFeatureLogCount(Long plantId, Long userId) {
        assertPlantOwnership(plantId, userId);
        return plantFeatureLogRepository.countByPowerPlantId(plantId);
    }

    public PlantFeatureLogSeriesDto getLatestFeatureLogs(Long plantId, Long userId, int limit) {
        assertPlantOwnership(plantId, userId);

        int safeLimit = Math.max(1, Math.min(limit, 500));
        List<PlantFeatureLogDto> latest = plantFeatureLogRepository
                .findByPowerPlantIdOrderByMeasuredAtDesc(
                        plantId,
                        PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "measuredAt"))
                )
                .stream()
                .map(this::toDto)
                .toList();

        return PlantFeatureLogSeriesDto.builder()
                .plantId(plantId)
                .series(latest)
                .build();
    }

    public PlantFeatureLogSeriesDto getFeatureLogSeries(Long plantId, Long userId, LocalDateTime from, LocalDateTime to) {
        assertPlantOwnership(plantId, userId);

        List<PlantFeatureLogDto> series = plantFeatureLogRepository
                .findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(plantId, from, to)
                .stream()
                .map(this::toDto)
                .toList();

        return PlantFeatureLogSeriesDto.builder()
                .plantId(plantId)
                .series(series)
                .build();
    }

    private void assertPlantOwnership(Long plantId, Long userId) {
        PowerPlant plant = powerPlantRepository.findByIdAndUserId(plantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        if (plant.getId() == null) {
            throw new ResourceNotFoundException("발전소를 찾을 수 없습니다.");
        }
    }

    private PlantFeatureLogDto toDto(PlantFeatureLog log) {
        return PlantFeatureLogDto.builder()
                .measuredAt(log.getMeasuredAt())
                .temp(log.getTemp())
                .humi(log.getHumi())
                .clou(log.getClou())
                .wisp(log.getWisp())
                .irradiance(log.getIrradiance())
                .prediction(log.getPrediction())
                .actual(log.getActual())
                .build();
    }
}

