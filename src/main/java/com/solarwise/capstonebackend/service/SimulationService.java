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

    // ✅ 대시보드 순환 재생을 위한 DB 데이터 시간 범위 캐시
    private volatile LocalDateTime dataMinTime = null;
    private volatile LocalDateTime dataMaxTime = null;
    private volatile boolean dataRangeInitialized = false;
    private static final Long DEFAULT_PLANT_ID = 1L; // 첫 번째 발전소 기준

    private final PowerPlantRepository powerPlantRepository;
    private final AnomalyRepository anomalyRepository;
    private final PlantFeatureLogRepository plantFeatureLogRepository;
    private final NotificationService notificationService;
    private final VisionAnalysisRepository visionAnalysisRepository;

    public synchronized LocalDateTime getVirtualCurrentTime() { return virtualCurrentTime; }

    public synchronized void advanceTimeByHour() {
        // ✅ 첫 호출 시 DB 데이터 범위 초기화
        if (!dataRangeInitialized) {
            initializeDataRange();
        }

        virtualCurrentTime = virtualCurrentTime.plusHours(1);

        // ✅ 순환 재생: 데이터의 끝을 초과하면 처음으로 리셋
        if (dataMaxTime != null && virtualCurrentTime.isAfter(dataMaxTime)) {
            if (dataMinTime != null) {
                virtualCurrentTime = dataMinTime;
                log.info("대시보드 순환 재생: 가상 시간이 DB 데이터 끝({})에 도달했으므로 처음({})으로 리셋",
                        dataMaxTime, dataMinTime);
                notifiedAnomalyIds.clear(); // 순환 시 알림 기록 초기화
            }
        }

        lastTickAt = virtualCurrentTime;
        log.info("가상 시간 1시간 전진: {}", virtualCurrentTime);
    }

    /**
     * ✅ DB 데이터의 시간 범위를 조회하여 순환 재생 경계값으로 설정
     */
    private synchronized void initializeDataRange() {
        try {
            // 첫 번째 발전소의 데이터 범위 조회
            PlantFeatureLog minLog = plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtAsc(DEFAULT_PLANT_ID);
            PlantFeatureLog maxLog = plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtDesc(DEFAULT_PLANT_ID);

            if (minLog != null && maxLog != null) {
                dataMinTime = minLog.getMeasuredAt();
                dataMaxTime = maxLog.getMeasuredAt();
                dataRangeInitialized = true;
                log.info("✅ 대시보드 순환 재생 범위 초기화: {} ~ {} (약 {}일)",
                        dataMinTime,
                        dataMaxTime,
                        (maxLog.getId() - minLog.getId()));
            } else {
                log.warn("⚠️ DB에 PlantFeatureLog 데이터가 없어 순환 재생 범위를 설정할 수 없습니다.");
                dataRangeInitialized = true; // 재시도 방지
            }
        } catch (Exception e) {
            log.error("❌ 대시보드 순환 재생 범위 초기화 중 오류: {}", e.getMessage(), e);
            dataRangeInitialized = true; // 재시도 방지
        }
    }

    public void startPlayback() {
        playbackRunning.set(true);
        dataRangeInitialized = false; // 재시작 시 데이터 범위 다시 초기화
        log.info("시뮬레이션 자동 재생 시작");
    }
    public void stopPlayback() { playbackRunning.set(false); log.info("시뮬레이션 자동 재생 정지"); }
    public boolean isPlaybackRunning() { return playbackRunning.get(); }
    public int getPlaybackTickSeconds() { return 1; }
    public int getPlaybackStepHours() { return 1; }
    public LocalDateTime getLastTickAt() { return lastTickAt; }
    public void triggerDroneError() { triggerNextError.set(true); log.info("드론 에러 트리거 설정됨"); }

    /**
     * ✅ 대시보드 순환 재생 상태 조회
     */
    public synchronized void getPlaybackStatus() {
        log.info("대시보드 순환 재생 상태 - 현재 시간: {}, 데이터 범위: {} ~ {}",
                virtualCurrentTime, dataMinTime, dataMaxTime);
    }

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
                .cause(String.format("미래 데이터 조작: %.2f%% 감소", (1 - reductionFactor) * 100))
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
