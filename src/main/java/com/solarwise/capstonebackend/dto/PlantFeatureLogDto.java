package com.solarwise.capstonebackend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 발전소 고급 피처 로그 응답 DTO
 */
@Data
@Builder
public class PlantFeatureLogDto {

    private LocalDateTime measuredAt;
    private Double temp;
    private Double humi;
    private Double clou;
    private Double wisp;
    private Double irradiance;
    private Double prediction;
    private Double actual;
}

