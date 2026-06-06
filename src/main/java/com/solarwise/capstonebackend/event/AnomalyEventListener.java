package com.solarwise.capstonebackend.event;

import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.VisionAnalysis;
import com.solarwise.capstonebackend.repository.VisionAnalysisRepository;
import com.solarwise.capstonebackend.service.AiIntegrationService;
import com.solarwise.capstonebackend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnomalyEventListener {

    private final AiIntegrationService aiIntegrationService;
    private final NotificationService notificationService;
    private final VisionAnalysisRepository visionAnalysisRepository;

    @Async
    @EventListener
    @Transactional
    public void handleAnomalyCreatedEvent(AnomalyCreatedEvent event) {
        Anomaly anomaly = event.getAnomaly();
        log.info("이상 현상 생성 이벤트 수신 (ID: {}), 후속 처리를 시작합니다.", anomaly.getId());

        // 1. 비전 이상일 경우, VisionAnalysis 엔티티 생성 및 AI 분석 요청
        if ("VISION".equals(anomaly.getType()) && anomaly.getImageUrl() != null) {
            
            // VisionAnalysis 엔티티 생성
            VisionAnalysis visionAnalysis = VisionAnalysis.builder()
                .anomaly(anomaly)
                .imageUrl(anomaly.getImageUrl())
                .analysisResult("AI 분석 대기 중")
                .createdAt(anomaly.getCreatedAt())
                .build();
            visionAnalysisRepository.save(visionAnalysis);
            
            String imageFileName = anomaly.getImageUrl().substring(anomaly.getImageUrl().lastIndexOf('/') + 1);
            try {
                aiIntegrationService.requestAnomalyDetail(anomaly.getPowerPlant().getId(), imageFileName).join();
                log.info("Anomaly ID {}에 대한 AI 상세 분석 요청 성공", anomaly.getId());
            } catch (Exception e) {
                log.error("비동기 AI 분석 요청 중 오류 발생 (Anomaly ID: {}): {}", anomaly.getId(), e.getMessage());
            }
        }

        // 2. 심각도가 HIGH일 경우, 이메일 알림 발송
        if ("HIGH".equals(anomaly.getSeverity())) {
            try {
                String ownerEmail = anomaly.getPowerPlant().getUser().getEmail();
                if (ownerEmail != null && !ownerEmail.isEmpty()) {
                    notificationService.sendAnomalyAlert(ownerEmail, anomaly);
                }
            } catch (Exception e) {
                log.error("비동기 알림 발송 중 오류 발생 (Anomaly ID: {}): {}", anomaly.getId(), e.getMessage());
            }
        }
    }
}
