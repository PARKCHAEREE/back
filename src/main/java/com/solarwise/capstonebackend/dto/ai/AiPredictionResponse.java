package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiPredictionResponse(
        @JsonProperty("plant_id") String plantId,
        @JsonProperty("predicted_ac_power") Double predictedAcPower,
        Double confidence,
        @JsonProperty("drift_detected") boolean driftDetected
) {
}