package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 이상 탐지 엔티티
 * - AI 모델이 탐지한 이상 상황 (발전량 저하, 패널 결함, 오염도 등)
 */
@Entity
@Table(name = "anomalies", indexes = {
        @Index(name = "idx_anomalies_power_plant_detected_at", columnList = "power_plant_id,detected_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Anomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "power_plant_id", nullable = false)
    private PowerPlant powerPlant;

    @Column(nullable = false, length = 50)
    private String type; // POWER, VISION

    @Column(nullable = false, length = 255)
    private String summary; // 요약

    @Column(length = 500)
    private String description; // 상세 설명

    @Column(nullable = false, length = 50)
    private String severity; // LOW, MEDIUM, HIGH

    @Column(length = 500)
    private String cause; // 원인

    @Column(length = 500)
    private String recommendedAction; // 권장 조치

    @Column(columnDefinition = "LONGTEXT")
    private String xaiExplanation; // SHAP/LIME 기반 설명 가능한 AI

    @Column(length = 50, columnDefinition = "VARCHAR(50) DEFAULT 'DETECTED'")
    private String status; // DETECTED, ACKNOWLEDGED, RESOLVED

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    @Column
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "DETECTED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
