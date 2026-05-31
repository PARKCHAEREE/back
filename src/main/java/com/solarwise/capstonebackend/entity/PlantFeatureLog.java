package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 자문가 고급 피처 로그 엔티티
 * - 최종 31개 피처 보존을 위한 테이블 매핑 (ddl-auto=update 로 컬럼 자동 생성)
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

    // 1. 기본 발전량 데이터
    @Column
    private Double actual;

    @Column
    private Double prediction;

    // 2. 기상/환경 변수
    @Column
    private Double temp;

    @Column
    private Double humi;

    @Column
    private Double clou;

    @Column
    private Double wisp;

    // 3. 시간 및 계절성 파생 변수
    @Column
    private Double hSin;

    @Column
    private Double hCos;

    @Column
    private Double doySin;

    @Column
    private Double doyCos;

    @Column
    private Double wideSin;

    @Column
    private Double wideCos;

    // 4. 일사량 및 발전소 용량 변수
    @Column
    private Double sunElevClip;

    @Column
    private Double cosZen;

    @Column
    private Double irradiance;

    @Column
    private Double estIrradiance;

    @Column
    private Double irradianceProxy;

    @Column
    private Double irradianceXCapa;

    @Column
    private Double capa;

    // 5. 발전량 패턴 및 과거 시계열 (LAG)
    @Column
    private Double seasonalSolarPattern;

    @Column
    private Double weatherAdjustedPattern;

    @Column
    private Double expectedGenProxy;

    @Column
    private Double genLag2;

    // 6. 🚨 이상 탐지 (비전/XAI) 관련 신규 피처
    @Column
    private Double dustCoverageRatio;

    @Column
    private Double snowCoverageRatio;

    @Column
    private Integer birdDroppingCount;

    @Column
    private Integer physicalDamageCount;

    @Column
    private Double maxDefectConfidence;

    @Column
    private Integer clsNormal;

    @Column
    private Integer clsDust;

    @Column
    private Integer clsSnow;

    @Column
    private Integer clsBird;

    @Column
    private Integer clsDamage;
}