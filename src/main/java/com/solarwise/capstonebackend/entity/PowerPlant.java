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
    private String inverterModel; // 인버터 모델

    @Column(length = 100)
    private String sensorSerialNumber; // 센서 시리얼 번호

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
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        active = true;
        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}

