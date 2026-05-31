package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 타임라인용 이상 이벤트 마커 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyMarkerDto {

    private Long eventId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime ts;

    private String type;

    private String severity;

    private String status;

    private String summary;
}

