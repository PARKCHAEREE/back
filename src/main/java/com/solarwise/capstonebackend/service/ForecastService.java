package com.solarwise.capstonebackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solarwise.capstonebackend.dto.ForecastExplanationDto;
import com.solarwise.capstonebackend.dto.ForecastResponseDto;
import com.solarwise.capstonebackend.dto.ai.ForecastDto;
import com.solarwise.capstonebackend.entity.Forecast;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.event.ForecastGenerationEvent;
import com.solarwise.capstonebackend.repository.ForecastRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastService {

    private final ForecastRepository forecastRepository;
    private final PowerPlantRepository powerPlantRepository;
    private final AiIntegrationService aiIntegrationService;
    private final ObjectMapper objectMapper;
    private final SimulationService simulationService;

    @Async
    @EventListener
    public void handleForecastGeneration(ForecastGenerationEvent event) {
        log.info("발전소 ID {}에 대한 AI 예측 데이터 생성을 시작합니다. (이벤트 수신)", event.getPlantId());

        aiIntegrationService.requestPowerForecast(event.getPlantId(), event.getTargetTime())
            .thenAccept(responseMap -> {
                if (responseMap == null || !responseMap.containsKey("series")) {
                    log.warn("AI 서버로부터 받은 예측 데이터에 'series' 키가 없습니다.");
                    return;
                }
                List<Map<String, Object>> series = (List<Map<String, Object>>) responseMap.get("series");
                if (series != null && !series.isEmpty()) {
                    saveForecasts(event.getPlantId(), series);
                }
            }).exceptionally(ex -> {
                log.error("AI 예측 데이터 생성 및 저장 중 오류 발생", ex);
                return null;
            });
    }

    @Transactional
    public void saveForecasts(Long plantId, List<Map<String, Object>> series) {
        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        powerPlantRepository.findById(plantId).ifPresent(plant -> {
            List<Forecast> forecasts = new ArrayList<>();
            
            // 💡 최종 수정: 향후 24시간치 데이터 3시간 간격으로 생성
            for (int i = 1; i <= 8; i++) {
                forecasts.add(Forecast.builder()
                        .powerPlant(plant)
                        .targetTime(virtualNow.plusHours(i * 3))  // 3, 6, 9...24시간 후
                        .predictedPowerKw(series.isEmpty() ? 0.0 :
                            ((Number) series.get(0).getOrDefault("predictedPowerKw", 0.0)).doubleValue())
                        .confidence(0.9)
                        .modelVersion("v1.0-real-data")
                        .status("COMPLETED")
                        .createdAt(LocalDateTime.now())
                        .build());
            }
            forecastRepository.saveAll(forecasts);
            log.info("발전량 예측 결과 {}건 저장 완료", forecasts.size());
        });
    }

    @Transactional(readOnly = true)
    public ForecastResponseDto getStoredForecasts(Long plantId, LocalDateTime from, LocalDateTime to) {
        PowerPlant plant = powerPlantRepository.findById(plantId)
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + plantId));

        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        LocalDateTime startTime = (from == null) ? virtualNow.minusDays(1) : from;
        LocalDateTime endTime = (to == null) ? virtualNow.plusDays(2) : to;

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
                .generatedAt(LocalDateTime.now())
                .series(series)
                .build();
    }

    @Cacheable(value = "forecastExplanations", key = "{#plantId, #targetTime.toString()}")
    @Transactional(readOnly = true)
    public ForecastExplanationDto getForecastExplanations(Long plantId, LocalDateTime targetTime) {
        log.info("캐시된 데이터 없음 - AI 서버에 예측 설명을 요청합니다. (plantId: {}, targetTime: {})", plantId, targetTime);
        try {
            Map<String, Object> aiResult = aiIntegrationService.requestPowerForecast(plantId, targetTime).join();

            if (aiResult == null) {
                log.warn("AI 서버로부터 예측 설명 데이터(null)를 받았습니다.");
                return createEmptyExplanation(targetTime);
            }

            Object factorsObj = aiResult.get("shap_bars");
            List<ForecastExplanationDto.Factor> factors = Collections.emptyList();
            if (factorsObj instanceof List) {
                List<Map<String, Object>> factorsMap = (List<Map<String, Object>>) factorsObj;
                factors = factorsMap.stream()
                        .map(bar -> {
                            String name = bar.get("label") instanceof String ? (String) bar.get("label") : "Unknown";
                            Double impact = bar.get("impact") instanceof Number ? ((Number) bar.get("impact")).doubleValue() : 0.0;
                            return ForecastExplanationDto.Factor.builder()
                                    .name(name)
                                    .impact(impact)
                                    .build();
                        })
                        .collect(Collectors.toList());
            }
            
            String summary = aiResult.get("summary") instanceof String ? (String) aiResult.get("summary") : "분석 요약을 가져올 수 없습니다.";

            return ForecastExplanationDto.builder()
                    .targetTime(targetTime)
                    .summary(summary)
                    .factors(factors)
                    .build();

        } catch (Exception e) {
            log.error("AI 예측 설명 요청 실패: {}", e.getMessage());
            return createEmptyExplanation(targetTime);
        }
    }

    private ForecastExplanationDto createEmptyExplanation(LocalDateTime targetTime) {
        return ForecastExplanationDto.builder()
                .targetTime(targetTime)
                .summary("AI 서버에서 분석 정보를 가져오는 데 실패했습니다.")
                .factors(Collections.emptyList())
                .build();
    }
}
