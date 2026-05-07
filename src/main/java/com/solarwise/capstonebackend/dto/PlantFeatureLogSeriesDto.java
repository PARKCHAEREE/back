package com.solarwise.capstonebackend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 발전소 고급 피처 시계열 응답 DTO
 */
@Data
@Builder
public class PlantFeatureLogSeriesDto {

    private Long plantId;
    private List<PlantFeatureLogDto> series;
}

