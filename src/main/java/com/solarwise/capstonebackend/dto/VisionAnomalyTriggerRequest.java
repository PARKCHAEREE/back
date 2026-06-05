package com.solarwise.capstonebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisionAnomalyTriggerRequest {
    private Long plantId;
    private String anomalyType;
    private String anomalySeverity; // HIGH, MEDIUM, LOW
    private Double confidence;
    private String imageUrl;
    private String xaiExplanation;
}
