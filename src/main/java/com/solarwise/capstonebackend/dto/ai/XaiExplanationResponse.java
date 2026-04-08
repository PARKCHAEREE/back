package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record XaiExplanationResponse(
        @JsonProperty("plant_id") String plantId,
        @JsonProperty("feature_importance") Map<String, Double> featureImportance,
        String summary
) {
}