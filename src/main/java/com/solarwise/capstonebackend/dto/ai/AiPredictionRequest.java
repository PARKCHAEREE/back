package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record AiPredictionRequest(
        @JsonProperty("plant_id") String plantId,
        LocalDateTime datetime,
        Double irradiation,
        @JsonProperty("ambient_temperature") Double ambientTemperature,
        @JsonProperty("module_temperature") Double moduleTemperature,
        @JsonProperty("wind_speed") Double windSpeed,
        Double humidity
) {
}