package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.PowerAnomalyTriggerRequest;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
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
import com.solarwise.capstonebackend.repository.VisionAnalysisRepository;
import com.solarwise.capstonebackend.entity.VisionAnalysis;

/**
 * 시뮬레이션 서비스
 * - 가상 시간 기반의 시뮬레이션 아키텍처를 제공합니다.
 * - 1분마다 가상 시간을 1시간씩 증가시키고, 드론 에러 트리거 시 이미지 이상 탐지를 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService {

    // 1초=1시간 재생 기준으로 트리거 후 약 5초 뒤 이상이 보이게 지연
    private static final int POWER_ANOMALY_DELAY_HOURS = 5;

    // 가상 현재 시간 (초기값: 2026-03-15 13:00:00)
    private LocalDateTime virtualCurrentTime = LocalDateTime.of(2026, 3, 15, 13, 0);

    // 드론 에러 트리거 플래그
    private final AtomicBoolean triggerNextError = new AtomicBoolean(false);

    // 시연용 자동 재생 상태
    private final AtomicBoolean playbackRunning = new AtomicBoolean(false);

    // 마지막 tick 시점(가상 시간 기준)
    private volatile LocalDateTime lastTickAt;

    // 메일 발송 여부를 메모리에 기록하여 중복 발송을 방지합니다.
    private final Set<Long> notifiedAnomalyIds = ConcurrentHashMap.newKeySet();

    private final PowerPlantRepository powerPlantRepository;
    private final AnomalyRepository anomalyRepository;
    private final PlantFeatureLogRepository plantFeatureLogRepository;
    private final NotificationService notificationService;
    private final VisionAnalysisRepository visionAnalysisRepository;

    /**
     * 가상 현재 시간을 반환합니다.
     *
     * @return 가상 현재 시간
     */
    public synchronized LocalDateTime getVirtualCurrentTime() {
        return virtualCurrentTime;
    }

    /**
     * 가상 시간을 1시간 앞으로 땡깁니다.
     */
    public synchronized void advanceTimeByHour() {
        virtualCurrentTime = virtualCurrentTime.plusHours(1);
        lastTickAt = virtualCurrentTime;
        log.info("가상 시간 1시간 전진: {}", virtualCurrentTime);
    }

    public void startPlayback() {
        playbackRunning.set(true);
        log.info("시뮬레이션 자동 재생 시작");
    }

    public void stopPlayback() {
        playbackRunning.set(false);
        log.info("시뮬레이션 자동 재생 정지");
    }

    public boolean isPlaybackRunning() {
        return playbackRunning.get();
    }

    public int getPlaybackTickSeconds() {
        return 1;
    }

    public int getPlaybackStepHours() {
        return 1;
    }

    public LocalDateTime getLastTickAt() {
        return lastTickAt;
    }

    /**
     * 드론 에러 트리거를 설정합니다.
     */
    public void triggerDroneError() {
        triggerNextError.set(true);
        log.info("드론 에러 트리거 설정됨");
    }

    /**
     * 시뮬레이션용 발전량 이상 트리거
     * - 트리거 시점 "이후"의 미래 데이터(actual)를 조작해 대시보드에서 시간이 지나며 이상이 관측되도록 합니다.
     * - Anomaly는 미래 시작 시점(detectedAt)으로 저장합니다.
     */
    @Transactional
    public Anomaly triggerPowerAnomaly(PowerAnomalyTriggerRequest request) {
        PowerPlant plant = powerPlantRepository.findById(request.getPlantId())
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + request.getPlantId()));

        LocalDateTime now = getVirtualCurrentTime();
        int durationHours = request.getDurationHours() == null ? 2 : Math.max(1, request.getDurationHours());
        double differencePercentage = request.getDifferencePercentage() == null ? 30.0 : Math.max(0.0, request.getDifferencePercentage());
        double reductionFactor = Math.max(0.0, 1.0 - (differencePercentage / 100.0));

        // 시연 자연스러움을 위해 트리거 후 5초(=가상시간 5시간) 지연 후부터 조작
        LocalDateTime anomalyStart = now.plusHours(POWER_ANOMALY_DELAY_HOURS);
        LocalDateTime anomalyEnd = anomalyStart.plusHours(durationHours - 1L);

        List<PlantFeatureLog> targetLogs = plantFeatureLogRepository
                .findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(plant.getId(), anomalyStart, anomalyEnd);

        int adjustedCount = 0;
        for (PlantFeatureLog featureLog : targetLogs) {
            Double baseline = featureLog.getPrediction() != null ? featureLog.getPrediction() : featureLog.getActual();
            if (baseline == null) {
                continue;
            }
            featureLog.setActual(baseline * reductionFactor);
            adjustedCount++;
        }
        plantFeatureLogRepository.saveAll(targetLogs);

        Anomaly anomaly = Anomaly.builder()
                .powerPlant(plant)
                .type("POWER")
                .summary("시뮬레이션 트리거: 발전량 이상")
                .description(request.getDescription())
                .severity(request.getAnomalySeverity())
                .cause(String.format("미래 데이터 조작: %.2f%% 감소, 기간 %s ~ %s, 적용 건수 %d",
                        differencePercentage,
                        anomalyStart,
                        anomalyEnd,
                        adjustedCount))
                .recommendedAction(null)
                .status("OPEN")
                .detectedAt(anomalyStart)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // 알림/관측은 대시보드 시간이 anomalyStart를 지날 때 시연 흐름에서 확인
        return anomalyRepository.save(anomaly);
    }

    /**
     * 시뮬레이션용 비전 이상 트리거
     * - VisionAnalysis 및 Anomaly 엔티티를 생성하여 저장하고 알림을 발송합니다.
     * - confidence 기준으로 severity를 결정합니다. (>=0.9 -> HIGH, else MEDIUM)
     */
    @Transactional
    public Anomaly triggerVisionAnomaly(com.solarwise.capstonebackend.dto.VisionAnomalyTriggerRequest request) {
        PowerPlant plant = powerPlantRepository.findById(request.getPlantId())
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + request.getPlantId()));

        LocalDateTime now = getVirtualCurrentTime();

        String severity = (request.getConfidence() != null && request.getConfidence() >= 0.9) ? "HIGH" : "MEDIUM";

        Anomaly anomaly = Anomaly.builder()
                .powerPlant(plant)
                .type("VISION")
                .summary("시뮬레이션 트리거: 비전 이상")
                .description(request.getXaiExplanation())
                .severity(severity)
                .cause(String.format("유형: %s, 신뢰도: %.2f", request.getAnomalyType(), request.getConfidence() == null ? 0.0 : request.getConfidence()))
                .recommendedAction(null)
                .status("OPEN")
                .detectedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Anomaly saved = anomalyRepository.save(anomaly);

        VisionAnalysis visionAnalysis = VisionAnalysis.builder()
                .anomaly(saved)
                .imageUrl(request.getImageUrl())
                .analysisResult(String.format("type:%s, confidence:%.2f, xai:%s",
                        request.getAnomalyType(), request.getConfidence() == null ? 0.0 : request.getConfidence(), request.getXaiExplanation()))
                .createdAt(now)
                .build();

        visionAnalysisRepository.save(visionAnalysis);

        // Lazy Loading 에러 방지: 이메일을 미리 추출한 후 전달
        String ownerEmail = plant.getUser().getEmail();
        notificationService.sendAnomalyAlert(ownerEmail, saved);

        return saved;
    }

    /**
     * 1초마다 실행되는 스케줄러
     * - 가상 시간을 1시간 증가
     * - 드론 에러 트리거가 설정된 경우 이미지 이상 탐지 수행
     */
    @Scheduled(fixedRate = 1000) // 1초 = 1000ms
    @Transactional
    public void simulateTimeProgression() {
        if (!playbackRunning.get()) {
            return;
        }

        // 가상 시간 1시간 증가
        advanceTimeByHour();

        // 드론 에러 트리거 확인
        if (triggerNextError.get()) {
            log.info("드론 에러 트리거 활성화: 파손 패널 이미지 분석 수행");
            // TODO: AiIntegrationService.detectVisionAnomaly 호출 (파손 이미지)
            // detectVisionAnomaly(plantId, "PANEL_001", damagedImageBase64);
            triggerNextError.set(false); // 트리거 리셋
        } else {
            log.debug("정상 패널 이미지 분석 수행");
            // TODO: AiIntegrationService.detectVisionAnomaly 호출 (정상 이미지)
            // detectVisionAnomaly(plantId, "PANEL_001", normalImageBase64);
        }

        // 시뮬레이션 시간 진행 후, 감지시점이 지난 OPEN 상태의 이상 이벤트를 확인하여
        // HIGH 등급이면 소유자에게 이메일을 발송합니다. DB 상태(status)는 절대 변경하지 않습니다.
        try {
            List<Anomaly> dueAnomalies = anomalyRepository.findByDetectedAtLessThanEqualAndStatus(virtualCurrentTime, "OPEN");
            for (Anomaly a : dueAnomalies) {
                if (a != null && "HIGH".equals(a.getSeverity()) && !notifiedAnomalyIds.contains(a.getId())) {
                    // 트랜잭션 경계 내에서 소유자 이메일을 안전하게 추출
                    String ownerEmail = a.getPowerPlant() != null && a.getPowerPlant().getUser() != null
                            ? a.getPowerPlant().getUser().getEmail() : null;
                    if (ownerEmail != null && !ownerEmail.isEmpty()) {
                        notificationService.sendAnomalyAlert(ownerEmail, a);
                        // DB 상태를 변경하지 않고 메모리에 발송 여부만 기록
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
