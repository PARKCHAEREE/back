package com.solarwise.capstonebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 이상 탐지 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDto {

    private Long id;
    private Long powerPlantId;
    private String type;
    private String description;
    private Double severity;
    private String xaiExplanation;
    private String status;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;

}

