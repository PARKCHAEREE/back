package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 시계열 그래프 포인트 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimePointDto {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime ts;

    private Double value;
}

