package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnomalyDto {

    @JsonProperty("eventId")
    private Long eventId;

    @JsonProperty("plantId")
    private Long plantId;
    
    private String type;
    private String severity;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime detectedAt;
    
    private String summary;
    private String status;
    private String cause;
    private String recommendedAction;
    private String xaiExplanation;
    private String heatmapUrl;
}
