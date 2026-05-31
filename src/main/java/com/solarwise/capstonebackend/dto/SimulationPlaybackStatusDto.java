package com.solarwise.capstonebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 시뮬레이션 재생 상태 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationPlaybackStatusDto {

    private boolean running;

    // 시연 정책: 1초마다 가상시간 1시간 전진
    private int tickSeconds;

    private int stepHours;

    private LocalDateTime virtualCurrentTime;

    private LocalDateTime lastTickAt;
}

