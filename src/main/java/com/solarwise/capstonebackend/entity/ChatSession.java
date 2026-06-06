package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB에 맞춰 Long 타입으로 복원

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "power_plant_id", nullable = false)
    private PowerPlant powerPlant;

    @Column(nullable = false)
    private String sessionTitle;

    private Long relatedEventId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
