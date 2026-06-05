package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    private Double capacity;

    @Column(nullable = false)
    private Integer panelCount;

    @Column(length = 100)
    private String inverterModel;

    @Column(length = 100)
    private String sensorSerialNumber;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "kma_grid_nx")
    private Integer kmaGridNx;

    @Column(name = "kma_grid_ny")
    private Integer kmaGridNy;

    @Column(name = "site_id", length = 50)
    private String siteId;

    @Column(length = 50, columnDefinition = "VARCHAR(50) DEFAULT 'ACTIVE'")
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
    }
}
