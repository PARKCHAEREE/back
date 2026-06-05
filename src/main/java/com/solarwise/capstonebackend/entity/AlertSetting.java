package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "alert_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    @Column(nullable = false)
    private boolean emailEnabled = true; // 이메일 알림 활성화 여부

    @Builder.Default
    @Column(nullable = false)
    private boolean smsEnabled = false; // SMS 알림 활성화 여부 (향후 확장용)

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String minimumSeverity = "HIGH"; // 알림을 받을 최소 심각도 (HIGH, MEDIUM, LOW)
}
