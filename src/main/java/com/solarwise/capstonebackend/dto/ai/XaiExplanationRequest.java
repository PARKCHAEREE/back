package com.solarwise.capstonebackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * XAI 설명 요청 DTO
 * - 명세서 6-4 기준으로 수정
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XaiExplanationRequest {

    @JsonProperty("plant_id")
    private String plantId; // 발전소 ID

    @JsonProperty("event_id")
    private String eventId; // 이벤트 ID (예: 이상 탐지 이벤트 ID)

    @JsonProperty("context")
    private XaiContext context; // XAI 설명을 위한 컨텍스트 정보

    /**
     * XAI 설명 컨텍스트 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class XaiContext {

        @JsonProperty("anomaly_type")
        private String anomalyType; // 이상 유형 (POWER, VISION)

        @JsonProperty("forecast")
        private Double forecast; // 예측값

        @JsonProperty("actual")
        private Double actual; // 실제값

        @JsonProperty("weather")
        private WeatherInfo weather; // 날씨 정보

        /**
         * 날씨 정보 내부 클래스
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class WeatherInfo {

            @JsonProperty("irradiation")
            private Double irradiation; // 일사량

            @JsonProperty("ambient_temperature")
            private Double ambientTemperature; // 주변 온도

            @JsonProperty("module_temperature")
            private Double moduleTemperature; // 모듈 온도

            @JsonProperty("wind_speed")
            private Double windSpeed; // 풍속

            @JsonProperty("humidity")
            private Double humidity; // 습도

        }
    }
}

