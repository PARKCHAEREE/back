package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 실측-예측 괴리 포인트 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapPointDto {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime ts;

    private Double absGap;

    private Double gapRate;
}

