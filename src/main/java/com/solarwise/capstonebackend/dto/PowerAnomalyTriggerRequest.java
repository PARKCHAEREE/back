package com.solarwise.capstonebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 시뮬레이션용 발전량 이상 트리거 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerAnomalyTriggerRequest {
    private Long plantId;
    private String anomalySeverity; // LOW, MEDIUM, HIGH
    private Double differencePercentage; // 예: 40.0
    private Integer durationHours; // 지속 시간 (시간 단위)
    private String description; // 상세 설명
}

