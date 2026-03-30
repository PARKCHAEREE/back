package com.solarwise.capstonebackend.util;

import com.solarwise.capstonebackend.entity.WeatherData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 기상 데이터 포매팅 유틸리티
 * - 공공 데이터 포털(기상청 API) 응답 포맷 변환
 */
@Slf4j
@Component
public class WeatherDataFormatterUtil {

    /**
     * 기상청 API 응답을 WeatherData 엔티티로 변환
     * TODO: 실제 기상청 API 응답 포맷에 맞게 구현
     */
    public WeatherData parseWeatherApiResponse(String apiResponse) {
        log.info("기상 데이터 파싱: {}", apiResponse);

        return WeatherData.builder()
                .temperature(0.0)
                .humidity(0.0)
                .irradiance(0.0)
                .cloudCover(0.0)
                .timestamp(LocalDateTime.now())
                .build();
    }

}

