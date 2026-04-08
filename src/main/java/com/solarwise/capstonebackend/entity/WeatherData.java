package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 기상 데이터 엔티티
 * - 공공 데이터 포털(기상청 API) 연동
 */
@Entity
@Table(name = "weather_data", indexes = {
        @Index(name = "idx_weather_data_power_plant_timestamp", columnList = "power_plant_id,timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "power_plant_id", nullable = false)
    private PowerPlant powerPlant;

    @Column(nullable = false)
    private Double temperature; // 기온 (℃)

    @Column(nullable = false)
    private Double humidity; // 습도 (%)

    @Column(nullable = false)
    private Double irradiance; // 일사량 (W/m²)

    @Column(nullable = false)
    private Double cloudCover; // 운량 (%)

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
