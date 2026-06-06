package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.PowerAnomalyTriggerRequest;
import com.solarwise.capstonebackend.dto.VisionAnomalyTriggerRequest;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.entity.VisionAnalysis; // import 추가
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.repository.VisionAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final int POWER_ANOMALY_DELAY_HOURS = 5;
    private LocalDateTime virtualCurrentTime = LocalDateTime.of(2026, 3, 15, 13, 0);
    private final AtomicBoolean triggerNextError = new AtomicBoolean(false);
    private final AtomicBoolean playbackRunning = new AtomicBoolean(false);
    private volatile LocalDateTime lastTickAt;
    private final Set<Long> notifiedAnomalyIds = ConcurrentHashMap.newKeySet();

    private final PowerPlantRepository powerPlantRepository;
    private final AnomalyRepository anomalyRepository;
    private final PlantFeatureLogRepository plantFeatureLogRepository;
    private final NotificationService notificationService;
    private final VisionAnalysisRepository visionAnalysisRepository;

    public synchronized LocalDateTime getVirtualCurrentTime() { return virtualCurrentTime; }
    public synchronized void advanceTimeByHour() {
        virtualCurrentTime = virtualCurrentTime.plusHours(1);
        lastTickAt = virtualCurrentTime;
        log.info("가상 시간 1시간 전진: {}", virtualCurrentTime);
    }
    public void startPlayback() { playbackRunning.set(true); log.info("시뮬레이션 자동 재생 시작"); }
    public void stopPlayback() { playbackRunning.set(false); log.info("시뮬레이션 자동 재생 정지"); }
    public boolean isPlaybackRunning() { return playbackRunning.get(); }
    public int getPlaybackTickSeconds() { return 1; }
    public int getPlaybackStepHours() { return 1; }
    public LocalDateTime getLastTickAt() { return lastTickAt; }
    public void triggerDroneError() { triggerNextError.set(true); log.info("드론 에러 트리거 설정됨"); }

    @Transactional
    public Anomaly triggerPowerAnomaly(PowerAnomalyTriggerRequest request) {
        PowerPlant plant = powerPlantRepository.findById(request.getPlantId())
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + request.getPlantId()));
        LocalDateTime now = getVirtualCurrentTime();
        int durationHours = request.getDurationHours() == null ? 2 : Math.max(1, request.getDurationHours());
        double reductionFactor = Math.max(0.0, 1.0 - ((request.getDifferencePercentage() == null ? 30.0 : request.getDifferencePercentage()) / 100.0));
        LocalDateTime anomalyStart = now.plusHours(POWER_ANOMALY_DELAY_HOURS);
        LocalDateTime anomalyEnd = anomalyStart.plusHours(durationHours - 1L);

        List<PlantFeatureLog> targetLogs = plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(plant.getId(), anomalyStart, anomalyEnd);
        for (PlantFeatureLog log : targetLogs) {
            Double baseline = log.getPrediction() != null ? log.getPrediction() : log.getActual();
            if (baseline != null) log.setActual(baseline * reductionFactor);
        }
        plantFeatureLogRepository.saveAll(targetLogs);

        Anomaly anomaly = Anomaly.builder()
                .powerPlant(plant).type("POWER").summary("시뮬레이션 트리거: 발전량 이상")
                .description(request.getDescription()).severity(request.getAnomalySeverity())
                .cause(String.format("미래 데이터 조작: %.2f%% 감소, 적용 건수 %d", (1 - reductionFactor) * 100, targetLogs.size()))
                .status("OPEN").detectedAt(anomalyStart).createdAt(now).updatedAt(now)
                .build();
        return anomalyRepository.save(anomaly);
    }

    @Transactional
    public Anomaly triggerVisionAnomaly(VisionAnomalyTriggerRequest request) {
        PowerPlant plant = powerPlantRepository.findById(request.getPlantId())
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + request.getPlantId()));
        LocalDateTime now = getVirtualCurrentTime();

        Anomaly anomaly = Anomaly.builder()
                .powerPlant(plant).type("VISION").summary("시뮬레이션 트리거: 비전 이상")
                .description(request.getXaiExplanation()).severity(request.getAnomalySeverity())
                .cause(String.format("유형: %s, 신뢰도: %.2f", request.getAnomalyType(), request.getConfidence()))
                .status("OPEN").detectedAt(now).createdAt(now).updatedAt(now)
                .build();
        Anomaly saved = anomalyRepository.save(anomaly);

        VisionAnalysis visionAnalysis = VisionAnalysis.builder()
                .anomaly(saved).imageUrl(request.getImageUrl())
                .analysisResult(String.format("type:%s, confidence:%.2f", request.getAnomalyType(), request.getConfidence()))
                .createdAt(now).build();
        visionAnalysisRepository.save(visionAnalysis);

        if ("HIGH".equalsIgnoreCase(request.getAnomalySeverity())) {
            String ownerEmail = plant.getUser().getEmail();
            notificationService.sendAnomalyAlert(ownerEmail, saved);
        }
        return saved;
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
