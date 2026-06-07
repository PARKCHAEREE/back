package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnomalyDto {
    private Long eventId;
    private Long plantId;
    private String type;
    private String severity;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime detectedAt;
    private String summary;
    private String status;
    private String cause;
    private String recommendedAction;
    private String imageUrl; // 💡 요구사항 반영: 필드 추가
    private String heatmapUrl;
}
