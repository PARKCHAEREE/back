package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    private Long eventId;

    private String type; // POWER, VISION

    private String severity; // LOW, MEDIUM, HIGH

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime detectedAt;

    private String summary;

    private String status; // OPEN, ACKNOWLEDGED, RESOLVED

    private String cause;

    private String recommendedAction;

    private String xaiExplanation;

}

