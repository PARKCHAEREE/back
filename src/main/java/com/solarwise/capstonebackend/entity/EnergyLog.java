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

    @Column(name = "energy_kwh")
    private Double energyKwh; // 측정 구간 발전량 (kWh)

    /**
     * 온도 (℃) — CSV 누락 시 보간값으로 채워짐.
     * 기상청 API 연동 전까지 CSV 실측값 사용.
     */
    @Column
    private Double temperature;

    /**
     * 일사량 (W/m²) — 현재 CSV에 직접값 없음.
     * 추후 기상청 API 또는 OpenWeather 과거예보 연동으로 채울 예정.
     */
    @Column
    private Double irradiance;

    /**
     * 습도 (%) — CSV 누락 시 보간값으로 채워짐.
     * 기상청 API 연동 전까지 CSV 실측값 사용.
     */
    @Column
    private Double humidity;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
