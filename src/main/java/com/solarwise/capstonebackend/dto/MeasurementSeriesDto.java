package com.solarwise.capstonebackend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MeasurementSeriesDto {
    private Long plantId;
    private List<MeasurementDto> series;
}
