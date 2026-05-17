package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.PowerAnomalyTriggerRequest;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    // 가상 현재 시간 (초기값: 2026-03-15 13:00:00)
    private LocalDateTime virtualCurrentTime = LocalDateTime.of(2026, 3, 15, 13, 0);

    // 드론 에러 트리거 플래그
    private final AtomicBoolean triggerNextError = new AtomicBoolean(false);

    private final PowerPlantRepository powerPlantRepository;
    private final AnomalyRepository anomalyRepository;
    private final NotificationService notificationService;
    private final VisionAnalysisRepository visionAnalysisRepository;

    /**
     * 가상 현재 시간을 반환합니다.
     *
     * @return 가상 현재 시간
     */
    public LocalDateTime getVirtualCurrentTime() {
        return virtualCurrentTime;
    }

    /**
     * 가상 시간을 1시간 앞으로 땡깁니다.
     */
    public void advanceTimeByHour() {
        virtualCurrentTime = virtualCurrentTime.plusHours(1);
        log.info("가상 시간 1시간 전진: {}", virtualCurrentTime);
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
     * - Anomaly 엔티티를 생성하고 저장한 뒤 알림을 발송합니다.
     * - 시간은 반드시 가상 시간을 사용합니다.
     */
    public Anomaly triggerPowerAnomaly(PowerAnomalyTriggerRequest request) {
        PowerPlant plant = powerPlantRepository.findById(request.getPlantId())
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + request.getPlantId()));

        LocalDateTime now = getVirtualCurrentTime();

        Anomaly anomaly = Anomaly.builder()
                .powerPlant(plant)
                .type("POWER")
                .summary("시뮬레이션 트리거: 발전량 이상")
                .description(request.getDescription())
                .severity(request.getAnomalySeverity())
                .cause(String.format("시뮬레이션: 차이율 %.2f%%, 지속시간 %dh",
                        request.getDifferencePercentage() == null ? 0.0 : request.getDifferencePercentage(),
                        request.getDurationHours() == null ? 0 : request.getDurationHours()))
                .recommendedAction(null)
                .status("OPEN")
                .detectedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Anomaly saved = anomalyRepository.save(anomaly);

        // Lazy Loading 에러 방지: 이메일을 미리 추출한 후 전달
        String ownerEmail = plant.getUser().getEmail();
        notificationService.sendAnomalyAlert(saved, ownerEmail);

        return saved;
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
        notificationService.sendAnomalyAlert(saved, ownerEmail);

        return saved;
    }

    /**
     * 1분마다 실행되는 스케줄러
     * - 가상 시간을 1시간 증가
     * - 드론 에러 트리거가 설정된 경우 이미지 이상 탐지 수행
     */
    @Scheduled(fixedRate = 60000) // 1분 = 60000ms
    public void simulateTimeProgression() {
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
    }
}
