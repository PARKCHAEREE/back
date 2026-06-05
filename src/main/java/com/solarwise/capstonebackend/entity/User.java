package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자(발전소 관리자) 엔티티
 * - 인증/인가 정보 관리
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String role; // ADMIN, MANAGER, USER

    @Column
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_logout_at")
    private LocalDateTime lastLogoutAt;

    @PrePersist
    protected void onCreate() {
        // 시간 설정은 Service 계층에서 수행 (SimulationService.getVirtualCurrentTime() 사용)
        active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        // 시간 설정은 Service 계층에서 수행
    }

}

