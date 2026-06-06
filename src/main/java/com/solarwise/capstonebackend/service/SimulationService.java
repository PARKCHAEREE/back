package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.PowerAnomalyTriggerRequest;
import com.solarwise.capstonebackend.dto.VisionAnomalyTriggerRequest;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.entity.VisionAnalysis;
import com.solarwise.capstonebackend.event.ForecastGenerationEvent;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.repository.VisionAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService {

    private static final LocalDateTime SIMULATION_START_TIME = LocalDateTime.of(2025, 9, 25, 0, 0);
    private static final LocalDateTime SIMULATION_END_TIME = LocalDateTime.of(2025, 11, 6, 23, 59);

    private LocalDateTime virtualCurrentTime = SIMULATION_START_TIME;
    private final AtomicBoolean playbackRunning = new AtomicBoolean(false);
    private volatile LocalDateTime lastTickAt;
    private final Set<Long> notifiedAnomalyIds = ConcurrentHashMap.newKeySet();

    private final PowerPlantRepository powerPlantRepository;
    private final AnomalyRepository anomalyRepository;
    private final PlantFeatureLogRepository plantFeatureLogRepository;
    private final NotificationService notificationService;
    private final VisionAnalysisRepository visionAnalysisRepository;
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
        lastTickAt = virtualCurrentTime;
        log.info("가상 시간 1시간 전진: {}", virtualCurrentTime);
    }
    
    public void startPlayback() { playbackRunning.set(true); log.info("시뮬레이션 자동 재생 시작"); }
    public void stopPlayback() { playbackRunning.set(false); log.info("시뮬레이션 자동 재생 정지"); }
    public boolean isPlaybackRunning() { return playbackRunning.get(); }
    public LocalDateTime getLastTickAt() { return lastTickAt; }

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
                .detectedAt(now) // 💡 현재 가상 시간으로 감지
                .createdAt(now)
                .updatedAt(now)
                .build();
            
        Anomaly savedAnomaly = anomalyRepository.save(anomaly);
        
        if ("HIGH".equals(request.getAnomalySeverity())) {
            notificationService.sendAnomalyAlert(plant.getUser().getEmail(), savedAnomaly);
        }
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
            .imageUrl(request.getImageUrl()) // 💡 프론트에서 온 원본 이미지 저장
            .createdAt(now)
            .updatedAt(now)
            .build();
            
        Anomaly savedAnomaly = anomalyRepository.save(anomaly);
        
        if ("HIGH".equals(severity)) {
            notificationService.sendAnomalyAlert(plant.getUser().getEmail(), savedAnomaly);
        }
        return savedAnomaly;
    }

    @Scheduled(fixedRate = 3600000) // 1시간에 한번
    public void generatePeriodicForecasts() {
        log.info("주기적인 예측 데이터 생성을 위한 이벤트를 발행합니다...");
        LocalDateTime forecastTime = getVirtualCurrentTime().plusHours(1);
        powerPlantRepository.findAll().forEach(plant -> {
            eventPublisher.publishEvent(new ForecastGenerationEvent(this, plant.getId(), forecastTime));
        });
    }

    @Scheduled(fixedRate = 1000)
    @Transactional
    public void simulateTimeProgression() {
        if (!playbackRunning.get()) return;
        advanceTimeByHour();
        try {
            List<Anomaly> dueAnomalies = anomalyRepository.findByDetectedAtLessThanEqualAndStatus(virtualCurrentTime, "OPEN");
            for (Anomaly a : dueAnomalies) {
                if (a != null && "HIGH".equals(a.getSeverity()) && !notifiedAnomalyIds.contains(a.getId())) {
                    String ownerEmail = a.getPowerPlant().getUser().getEmail();
                    if (ownerEmail != null && !ownerEmail.isEmpty()) {
                        notificationService.sendAnomalyAlert(ownerEmail, a);
                        notifiedAnomalyIds.add(a.getId());
                        log.info("Sent notification for anomaly ID {} and recorded in-memory as notified", a.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while processing due anomalies on tick: {}", e.getMessage(), e);
        }
    }
}
