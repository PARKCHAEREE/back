package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 발전량 예측 엔티티
 * - AI 모델이 생성한 태양광 발전량 예측 데이터
 */
@Entity
@Table(name = "forecasts", indexes = {
        @Index(name = "idx_forecasts_power_plant_target_time", columnList = "power_plant_id,target_time"),
        @Index(name = "idx_forecasts_target_time", columnList = "target_time")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Forecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "power_plant_id", nullable = false)
    private PowerPlant powerPlant;

    @Column(nullable = false)
    private LocalDateTime targetTime; // 예측 대상 시간

    @Column(nullable = false)
    private Double predictedPowerKw; // 예측 발전량 (kW)

    @Column(nullable = false)
    private Double confidence; // 신뢰도 (0.0 ~ 1.0)

    @Column(length = 50)
    private String modelVersion; // 사용된 모델 버전

    @Column(length = 500)
    private String modelNotes; // 모델 관련 메모

    @Column
    private Double actualPowerKw; // 실제 발전량 (나중에 업데이트)

    @Column
    private Double mae; // 평균 절대 오차 (MAE)

    @Column
    private Double rmse; // 제곱근 평균 제곱 오차 (RMSE)

    @Column(length = 50, columnDefinition = "VARCHAR(50) DEFAULT 'PENDING'")
    private String status; // PENDING, COMPLETED, VERIFIED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}

