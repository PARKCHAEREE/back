package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 에너지 로그 엔티티
 * - 실시간 발전량 데이터 (시계열 시간당/분당)
 */
@Entity
@Table(name = "energy_logs", indexes = {
        @Index(name = "idx_energy_logs_power_plant_timestamp", columnList = "power_plant_id,timestamp"),
        @Index(name = "idx_energy_logs_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnergyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "power_plant_id", nullable = false)
    private PowerPlant powerPlant;

    @Column(nullable = false)
    private Double powerKw; // 실제 발전 전력 (kW)

    @Column(nullable = false)
    private Double temperature; // 온도 (℃)

    @Column(nullable = false)
    private Double irradiance; // 일사량 (W/m²)

    @Column(nullable = false)
    private Double humidity; // 습도 (%)

    @Column
    private Double predictedGeneration; // AI 예측 발전량 (kWh)

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
