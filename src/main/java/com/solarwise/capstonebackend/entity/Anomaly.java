package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "anomalies", indexes = {
        @Index(name = "idx_powerplant_detectedat", columnList = "power_plant_id, detected_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Anomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "power_plant_id", nullable = false)
    private PowerPlant powerPlant;

    @Column(nullable = false)
    private String type; // POWER, VISION

    @Column(nullable = false)
    private String severity; // LOW, MEDIUM, HIGH

    @Column(nullable = false)
    private String status; // OPEN, ACKNOWLEDGED, RESOLVED

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    private LocalDateTime resolvedAt;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String cause;

    @Column(columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(columnDefinition = "TEXT")
    private String xaiExplanation;

    @Column(name = "image_url", length = 500) // 💡 요구사항 반영
    private String imageUrl;

    @Column(name = "heatmap_url", length = 500) // 💡 요구사항 반영
    private String heatmapUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
