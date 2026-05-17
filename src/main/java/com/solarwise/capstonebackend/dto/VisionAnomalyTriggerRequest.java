package com.solarwise.capstonebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 시뮬레이션용 비전 이상 트리거 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisionAnomalyTriggerRequest {
    private Long plantId;
    private String anomalyType; // NORMAL, CRACK, DIRT
    private Double confidence;
    private String imageUrl;
    private String xaiExplanation; // 설명 또는 분석 근거
}

