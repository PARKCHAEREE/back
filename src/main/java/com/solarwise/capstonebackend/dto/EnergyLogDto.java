package com.solarwise.capstonebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 에너지 로그 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnergyLogDto {

    private Long id;
    private Long powerPlantId;
    private Double actualGeneration;
    private Double predictedGeneration;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;

}

