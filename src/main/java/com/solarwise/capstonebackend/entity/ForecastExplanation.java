package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 발전량 예측 근거 설명 엔티티
 * - AI 모델의 예측 근거 및 설명 데이터
 */
@Entity
@Table(name = "forecast_explanations", indexes = {
        @Index(name = "idx_forecast_explanations_forecast_id", columnList = "forecast_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastExplanation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forecast_id", nullable = false)
    private Forecast forecast;

    @Column(length = 2000)
    private String explanation; // AI 모델의 예측 근거 설명

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        // 시간 설정은 Service 계층에서 수행 (SimulationService.getVirtualCurrentTime() 사용)
    }
}
