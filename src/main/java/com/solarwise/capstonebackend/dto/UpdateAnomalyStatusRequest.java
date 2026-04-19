package com.solarwise.capstonebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이상 이벤트 상태 변경 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAnomalyStatusRequest {

    @NotBlank(message = "status는 필수입니다.")
    @Pattern(regexp = "OPEN|ACKNOWLEDGED|RESOLVED", message = "status는 OPEN, ACKNOWLEDGED, RESOLVED 중 하나여야 합니다.")
    private String status;
}

