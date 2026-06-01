package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 과거 데이터 DTO
 * - AI 모델 학습 및 예측에 사용되는 히스토리 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryDataDto {

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp; // 측정 시간

    @JsonProperty("actual_power_kw")
    private Double actualPowerKw; // 실제 발전량 (kW)

    @JsonProperty("irradiation")
    private Double irradiation; // 일사량 (0.0 ~ 1.0)

    @JsonProperty("ambient_temperature")
    private Double ambientTemperature; // 주변 온도 (°C)

    @JsonProperty("module_temperature")
    private Double moduleTemperature; // 모듈 온도 (°C)

    @JsonProperty("wind_speed")
    private Double windSpeed; // 풍속 (m/s)

    @JsonProperty("humidity")
    private Double humidity; // 습도 (%)

    @JsonProperty("dust_coverage_ratio")
    private Double dustCoverageRatio;

    @JsonProperty("snow_coverage_ratio")
    private Double snowCoverageRatio;

    @JsonProperty("bird_dropping_count")
    private Integer birdDroppingCount;

    @JsonProperty("physical_damage_count")
    private Integer physicalDamageCount;

    @JsonProperty("max_defect_confidence")
    private Double maxDefectConfidence;

    @JsonProperty("cls_normal")
    private Integer clsNormal;

    @JsonProperty("cls_dust")
    private Integer clsDust;

    @JsonProperty("cls_snow")
    private Integer clsSnow;

    @JsonProperty("cls_bird")
    private Integer clsBird;

    @JsonProperty("cls_damage")
    private Integer clsDamage;


}

