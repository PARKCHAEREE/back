package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 측정 데이터 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementDto {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime measuredAt;

    private Double powerKw; // 발전 전력 (kW)

    private Double temperature; // 온도 (℃)

    private Double irradiance; // 일사량 (W/m²)

    private Double humidity; // 습도 (%)

}

