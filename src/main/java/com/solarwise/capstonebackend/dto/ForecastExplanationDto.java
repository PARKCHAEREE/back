package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ForecastExplanationDto {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime targetTime;
    private String summary;
    private List<Factor> factors;

    @Data
    @Builder
    public static class Factor {
        private String name;
        private Double impact;
        private String description;
    }
}
