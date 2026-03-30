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
        @Index(name = "idx_power_plant_timestamp", columnList = "power_plant_id,detected_at")
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
    private String type; // GENERATION_DECREASE, PANEL_DEFECT, SOILING, etc.

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false)
    private Double severity; // 심각도 (0~1)

    @Column(columnDefinition = "LONGTEXT")
    private String xaiExplanation; // SHAP/LIME 기반 설명 가능한 AI

    @Column(length = 50)
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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}

