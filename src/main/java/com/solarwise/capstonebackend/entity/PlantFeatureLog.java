package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 자문가 고급 피처 로그 엔티티
 * - 원천 CSV의 예측 입력 피처를 보존하기 위한 테이블 매핑
 */
@Entity
@Table(name = "plant_feature_logs", indexes = {
        @Index(name = "idx_feature_logs_plant_time", columnList = "power_plant_id,measured_at"),
        @Index(name = "idx_feature_logs_time", columnList = "measured_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantFeatureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "power_plant_id", nullable = false)
    private Long powerPlantId;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    @Column
    private Double temp;

    @Column
    private Double humi;

    @Column
    private Double clou;

    @Column
    private Double wisp;

    @Column
    private Double irradiance;

    @Column
    private Double prediction;

    @Column
    private Double actual;
}

