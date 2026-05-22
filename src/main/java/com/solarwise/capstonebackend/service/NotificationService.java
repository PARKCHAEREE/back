package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.entity.Anomaly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 알림 서비스
 * - 이상 탐지 시 이메일 알림 발송 (네이버 SMTP 연동)
 * - 비동기 처리로 API 응답 지연 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender javaMailSender;

    // application.properties에서 네이버 아이디를 읽어옵니다.
    @Value("${spring.mail.username}")
    private String senderUsername;

    /**
     * 이상 탐지 알림 발송 (HIGH 등급만)
     * - 비동기 처리로 API 응답이 지연되지 않도록 함
     * - Lazy Loading 에러 방지를 위해 이메일을 파라미터로 받음
     *
     * @param anomaly 이상 탐지 정보
     * @param toEmail 수신자 이메일 주소
     */
    @Async
    public void sendAnomalyAlert(Anomaly anomaly, String toEmail) {
        try {
            // HIGH 등급이 아니면 발송 안 함
            if (anomaly == null || !"HIGH".equals(anomaly.getSeverity())) {
                return;
            }

            if (toEmail == null || toEmail.trim().isEmpty()) {
                log.warn("Anomaly ID {} has no recipient email address", anomaly.getId());
                return;
            }

            String mailSubject = "[HIGH] " + anomaly.getPowerPlant().getName() + " - " + anomaly.getDescription();
            String mailBody = buildEmailBody(anomaly);

            sendEmail(toEmail, mailSubject, mailBody);
            log.info("Successfully sent anomaly alert email to {} for anomaly ID {}", toEmail, anomaly.getId());

        } catch (Exception e) {
            log.error("Failed to send anomaly alert email", e);
        }
    }

    /**
     * 이메일 발송 (내부 메서드)
     *
     * @param to 수신자 이메일 주소
     * @param subject 메일 제목
     * @param body 메일 본문
     */
    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();

        // 네이버 SMTP 필수 규칙: 보내는 사람은 반드시 "네이버아이디@naver.com" 형태여야 함
        message.setFrom(senderUsername);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        javaMailSender.send(message);
    }

    /**
     * 이메일 본문 구성
     *
     * @param anomaly 이상 탐지 정보
     * @return 이메일 본문
     */
    private String buildEmailBody(Anomaly anomaly) {
        StringBuilder body = new StringBuilder();

        body.append("안녕하세요, SolarWise 팀입니다.\n\n");
        body.append("발전소 모니터링 중 HIGH 등급의 이상이 감지되었습니다.\n\n");

        body.append("=== 이상 탐지 정보 ===\n");
        body.append("발전소명: ").append(anomaly.getPowerPlant().getName()).append("\n");
        body.append("이상 유형: ").append(anomaly.getType()).append("\n");
        body.append("심각도: ").append(anomaly.getSeverity()).append("\n");
        body.append("요약: ").append(anomaly.getSummary()).append("\n");
        body.append("상세 설명: ").append(anomaly.getDescription()).append("\n\n");

        if (anomaly.getCause() != null && !anomaly.getCause().isEmpty()) {
            body.append("원인: ").append(anomaly.getCause()).append("\n\n");
        }

        if (anomaly.getRecommendedAction() != null && !anomaly.getRecommendedAction().isEmpty()) {
            body.append("권장 조치: ").append(anomaly.getRecommendedAction()).append("\n\n");
        }

        if (anomaly.getXaiExplanation() != null && !anomaly.getXaiExplanation().isEmpty()) {
            body.append("AI 분석 근거: ").append(anomaly.getXaiExplanation()).append("\n\n");
        }

        body.append("감지 일시: ").append(anomaly.getDetectedAt()).append("\n");
        body.append("상태: ").append(anomaly.getStatus()).append("\n\n");

        body.append("대시보드에서 자세한 내용을 확인하시기 바랍니다.\n");
        body.append("SolarWise 팀 드림");

        return body.toString();
    }
}