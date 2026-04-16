package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 서버의 설명 가능한 AI (XAI) 응답 DTO
 * - SHAP / LIME 기반 모델 설명 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XaiExplanationResponse {

    @JsonProperty("plant_id")
    private String plantId; // 발전소 ID

    @JsonProperty("explanations")
    private List<XaiExplanationDto> explanations; // XAI 설명 데이터

}