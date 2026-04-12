package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 설명 가능한 AI (XAI) 정보 DTO
 * - SHAP / LIME 기반 모델 예측 설명 및 특성 중요도
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XaiExplanationDto {

    @JsonProperty("feature_importance")
    private Map<String, Double> featureImportance; // 특성 중요도 (SHAP values)

    @JsonProperty("shap_values")
    private Map<String, Double> shapValues; // SHAP 값들

    @JsonProperty("lime_explanation")
    private String limeExplanation; // LIME 기반 텍스트 설명

    @JsonProperty("model_confidence")
    private Double modelConfidence; // 모델 신뢰도

    @JsonProperty("explanation_text")
    private String explanationText; // 인간이 읽을 수 있는 설명

}

