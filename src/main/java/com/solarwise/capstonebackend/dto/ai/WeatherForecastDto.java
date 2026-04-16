package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 날씨 예보 DTO
 * - 발전량 예측에 사용되는 미래 날씨 데이터
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherForecastDto {

    @JsonProperty("forecast_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime forecastTime; // 예보 시간

    @JsonProperty("irradiation")
    private Double irradiation; // 예상 일사량 (0.0 ~ 1.0)

    @JsonProperty("ambient_temperature")
    private Double ambientTemperature; // 예상 주변 온도 (°C)

    @JsonProperty("module_temperature")
    private Double moduleTemperature; // 예상 모듈 온도 (°C)

    @JsonProperty("wind_speed")
    private Double windSpeed; // 예상 풍속 (m/s)

    @JsonProperty("humidity")
    private Double humidity; // 예상 습도 (%)

    @JsonProperty("cloud_cover")
    private Double cloudCover; // 구름 덮음 비율 (0.0 ~ 1.0)

}

