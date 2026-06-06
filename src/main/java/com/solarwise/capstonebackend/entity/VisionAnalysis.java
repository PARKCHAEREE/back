package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vision_analyses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisionAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anomaly_id", nullable = false, unique = true)
    private Anomaly anomaly;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 2000)
    private String analysisResult;

    // heatmapUrl 필드를 제거하여 DB 스키마를 변경하지 않음

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
