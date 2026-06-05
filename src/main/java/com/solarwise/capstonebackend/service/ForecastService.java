package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.ForecastResponseDto;
import com.solarwise.capstonebackend.entity.Forecast;
import com.solarwise.capstonebackend.repository.ForecastRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForecastService {

    private final ForecastRepository forecastRepository;
    private final PowerPlantRepository powerPlantRepository;
    private final SimulationService simulationService;

    @Transactional(readOnly = true)
    public ForecastResponseDto getForecasts(Long plantId, LocalDateTime from, LocalDateTime to) {
        powerPlantRepository.findById(plantId)
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + plantId));

        LocalDateTime now = simulationService.getVirtualCurrentTime();
        LocalDateTime startTime = (from == null) ? now : from;
        LocalDateTime endTime = (to == null) ? startTime.plusDays(3) : to;

        List<Forecast> forecasts = forecastRepository.findByPowerPlantIdAndTargetTimeBetween(plantId, startTime, endTime);

        List<ForecastResponseDto.ForecastItem> series = forecasts.stream()
                .map(f -> ForecastResponseDto.ForecastItem.builder()
                        .targetTime(f.getTargetTime())
                        .predictedPowerKw(f.getPredictedPowerKw())
                        .confidence(f.getConfidence())
                        .build())
                .collect(Collectors.toList());

        return ForecastResponseDto.builder()
                .plantId(plantId)
                .generatedAt(now)
                .series(series)
                .build();
    }
}
