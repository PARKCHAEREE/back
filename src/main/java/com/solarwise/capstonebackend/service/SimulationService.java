package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.PowerAnomalyTriggerRequest;
import com.solarwise.capstonebackend.dto.VisionAnomalyTriggerRequest;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.event.AnomalyCreatedEvent;
import com.solarwise.capstonebackend.event.ForecastGenerationEvent;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean; // 💡 최종 수정: 누락된 import 구문 추가

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService {

    private static final LocalDateTime SIMULATION_START_TIME = LocalDateTime.of(2025, 9, 25, 0, 0);
    private static final LocalDateTime SIMULATION_END_TIME = LocalDateTime.of(2025, 11, 6, 23, 59);

    private LocalDateTime virtualCurrentTime = SIMULATION_START_TIME;
    private final AtomicBoolean playbackRunning = new AtomicBoolean(false);

    private final PowerPlantRepository powerPlantRepository;
    private final AnomalyRepository anomalyRepository;
    private final ApplicationEventPublisher eventPublisher;

    public synchronized LocalDateTime getVirtualCurrentTime() { return virtualCurrentTime; }

    public synchronized void advanceTimeByHour() {
        LocalDateTime nextTime = virtualCurrentTime.plusHours(1);
        if (nextTime.isAfter(SIMULATION_END_TIME)) {
            virtualCurrentTime = SIMULATION_START_TIME;
            log.info("시뮬레이션 시간 초기화: {}", virtualCurrentTime);
        } else {
            virtualCurrentTime = nextTime;
        }
        log.info("가상 시간 1시간 전진: {}", virtualCurrentTime);

        powerPlantRepository.findAll().forEach(plant -> {
            eventPublisher.publishEvent(new ForecastGenerationEvent(this, plant.getId(), virtualCurrentTime));
        });
    }
    
    public void startPlayback() { playbackRunning.set(true); log.info("시뮬레이션 자동 재생 시작"); }
    public void stopPlayback() { playbackRunning.set(false); log.info("시뮬레이션 자동 재생 정지"); }
    public boolean isPlaybackRunning() { return playbackRunning.get(); }

    @Transactional
    public Anomaly triggerPowerAnomaly(PowerAnomalyTriggerRequest request) {
        PowerPlant plant = powerPlantRepository.findById(request.getPlantId())
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + request.getPlantId()));
        
        LocalDateTime now = getVirtualCurrentTime();
        
        Anomaly anomaly = Anomaly.builder()
                .powerPlant(plant)
                .type("POWER")
                .severity(request.getAnomalySeverity())
                .status("OPEN")
                .summary(String.format("예상 대비 발전량 %.1f%% 변동", request.getDifferencePercentage()))
                .description(request.getDescription())
                .detectedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
            
        Anomaly savedAnomaly = anomalyRepository.save(anomaly);
        
        eventPublisher.publishEvent(new AnomalyCreatedEvent(this, savedAnomaly));
        
        return savedAnomaly;
    }

    @Transactional
    public Anomaly triggerVisionAnomaly(VisionAnomalyTriggerRequest request) {
        PowerPlant plant = powerPlantRepository.findById(request.getPlantId())
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + request.getPlantId()));
        
        LocalDateTime now = getVirtualCurrentTime();
        
        String severity = ("CRACK".equals(request.getAnomalyType()) || request.getConfidence() >= 0.9) 
                          ? "HIGH" : "MEDIUM";

        Anomaly anomaly = Anomaly.builder()
            .powerPlant(plant)
            .type("VISION")
            .severity(severity)
            .status("OPEN")
            .summary(String.format("비전 AI 감지: %s (신뢰도 %.1f%%)", request.getAnomalyType(), request.getConfidence() * 100))
            .description(request.getXaiExplanation())
            .detectedAt(now)
            .imageUrl(request.getImageUrl())
            .createdAt(now)
            .updatedAt(now)
            .build();
            
        Anomaly savedAnomaly = anomalyRepository.save(anomaly);
        
        eventPublisher.publishEvent(new AnomalyCreatedEvent(this, savedAnomaly));
        
        return savedAnomaly;
    }

    @Scheduled(fixedRate = 1000)
    public void simulateTimeProgression() {
        if (!playbackRunning.get()) return;
        advanceTimeByHour();
    }
}
