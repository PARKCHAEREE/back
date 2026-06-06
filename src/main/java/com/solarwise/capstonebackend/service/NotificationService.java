package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.entity.Anomaly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:}")
    private String senderUsername;

    @Async
    public void sendAnomalyAlert(String toEmail, Anomaly anomaly) {
        if (anomaly == null || !"HIGH".equals(anomaly.getSeverity())) {
            return;
        }
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Anomaly ID {} has no recipient email address, skipping notification.", anomaly.getId());
            return;
        }

        try {
            String mailSubject = "[SOLARWISE 경고] " + anomaly.getPowerPlant().getName() + " - 이상 감지 알림";
            String mailBody = buildEmailBodyForNaver(anomaly);
            sendEmail(toEmail, mailSubject, mailBody);
        } catch (Exception e) {
            // sendEmail 내부에서 이미 예외를 처리하고 로깅하므로, 여기서는 상위 호출자에게 전파되지 않도록만 함
            log.error("Failed to process anomaly alert for Anomaly ID {}. Error: {}", anomaly.getId(), e.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String body) {
        if (javaMailSender == null) {
            log.warn("JavaMailSender not configured - skipping email send to {}", to);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            String fromAddress = senderUsername != null && senderUsername.contains("@") ? senderUsername : senderUsername + "@naver.com";
            
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            javaMailSender.send(message);
            log.info("Successfully sent email to {}", to);

        } catch (MailException e) {
            // ⭐️ 요구사항 해결: 메일 발송 실패가 다른 트랜잭션에 영향을 주지 않도록 예외를 여기서 처리 ⭐️
            log.error("Failed to send email to {}. Subject: {}. Error: {}", to, subject, e.getMessage());
        }
    }

    private String buildEmailBodyForNaver(Anomaly anomaly) {
        StringBuilder sb = new StringBuilder();
        sb.append("안녕하세요. SolarWise 알림입니다.\n\n");
        sb.append("🚨 심각도: ").append(anomaly.getSeverity()).append("\n\n");
        sb.append("📊 이상 요약: ").append(anomaly.getSummary() == null ? "-" : anomaly.getSummary()).append("\n\n");
        sb.append("🔍 추정 원인: ").append(anomaly.getCause() == null ? "-" : anomaly.getCause()).append("\n\n");
        sb.append("🛠️ 권장 조치: ").append(anomaly.getRecommendedAction() == null ? "-" : anomaly.getRecommendedAction()).append("\n\n");
        sb.append("발전소: ").append(anomaly.getPowerPlant() != null ? anomaly.getPowerPlant().getName() : "-").append("\n");
        sb.append("감지 일시: ").append(anomaly.getDetectedAt()).append("\n\n");
        sb.append("자세한 내용은 대시보드에서 확인하세요.\n");
        sb.append("SolarWise 팀");
        return sb.toString();
    }
}
