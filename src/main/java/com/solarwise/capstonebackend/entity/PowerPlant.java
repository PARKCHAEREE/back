package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 발전소 엔티티
 * - 태양광 발전소의 기본 정보
 */
@Entity
@Table(name = "power_plants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerPlant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 100)
    private String location;

    @Column(nullable = false)
    private Double capacity; // kW 단위

    @Column(nullable = false)
    private Integer panelCount; // 패널 개수

    @Column(length = 100)
    private String inverterModel; // 인버터 모델 및 수량 (예: "986.375kW 인버터 × 11대")

    @Column(length = 100)
    private String sensorSerialNumber; // 센서 시리얼 번호

    /** 기상청 API 연동용 - 발전소 GPS 위도 (V_GPS_Y) */
    @Column(name = "latitude")
    private Double latitude;

    /** 기상청 API 연동용 - 발전소 GPS 경도 (V_GPS_X) */
    @Column(name = "longitude")
    private Double longitude;

    /**
     * 기상청 단기예보 격자 X 좌표 (nx).
     * Lambert Conformal Conic 변환값 - 충남 서천군 기준 약 56.
     * KMA 격자 변환 도구로 정확한 값 확인 후 업데이트 필요.
     */
    @Column(name = "kma_grid_nx")
    private Integer kmaGridNx;

    /**
     * 기상청 단기예보 격자 Y 좌표 (ny).
     * Lambert Conformal Conic 변환값 - 충남 서천군 기준 약 65.
     * KMA 격자 변환 도구로 정확한 값 확인 후 업데이트 필요.
     */
    @Column(name = "kma_grid_ny")
    private Integer kmaGridNy;

    /** 자문가 제공 CSV의 V_SITE_ID (예: KR10025001) */
    @Column(name = "site_id", length = 50)
    private String siteId;

    @Column(length = 50, columnDefinition = "VARCHAR(50) DEFAULT 'ACTIVE'")
    private String status; // ACTIVE, INACTIVE 등

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 발전소 관리자

    @Column(columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        // 시간 설정은 Service 계층에서 수행 (SimulationService.getVirtualCurrentTime() 사용)
        active = true;
        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // 시간 설정은 Service 계층에서 수행
    }

}