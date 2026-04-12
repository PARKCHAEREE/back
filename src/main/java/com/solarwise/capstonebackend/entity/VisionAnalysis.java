package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 이미지 분석 결과 엔티티
 * - AI 비전 모델의 패널 이미지 분석 결과
 */
@Entity
@Table(name = "vision_analyses", indexes = {
        @Index(name = "idx_vision_analyses_anomaly_id", columnList = "anomaly_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisionAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anomaly_id", nullable = false)
    private Anomaly anomaly;

    @Column(length = 500)
    private String imageUrl; // 분석된 이미지 URL

    @Column(length = 2000)
    private String analysisResult; // AI 분석 결과 상세

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
    }
}
