package com.solarwise.capstonebackend.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.entity.WeatherData;
import com.solarwise.capstonebackend.exception.BusinessException;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 기상 데이터 포매팅 유틸리티
 * - 공공 데이터 포털(기상청 API) 응답 포맷 변환
 */
@Slf4j
@Component
public class WeatherDataFormatterUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();


    public Map<String, Double> formatWeatherData(String jsonResponse) {
        Map<String, Double> parsedData = new HashMap<>();

        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode itemsNode = rootNode.path("response").path("body").path("items").path("item");

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    String category = item.path("category").asText();
                    double value = item.path("fcstValue").asDouble();

                    switch (category) {
                        case "TMP": parsedData.put("temperature", value); break;
                        case "REH": parsedData.put("humidity", value); break;
                        case "SKY": parsedData.put("cloudCover", value); break;
                    }
                }
            }

            parsedData.putIfAbsent("irradiance", 800.0); // 일사량 기본값

            log.info("기상 데이터 포매팅 완료: {}", parsedData);
            return parsedData;

        } catch (Exception e) {
            log.error("기상 데이터 변환 실패: {}", e.getMessage());
            throw new RuntimeException("기상청 응답 데이터를 처리하는 중 오류가 발생했습니다.", e);
        }
    }

    public WeatherData parseKmaResponse(String jsonResponse, PowerPlant plant) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode itemsNode = rootNode.path("response").path("body").path("items").path("item");

            Double temperature = null;
            Double humidity = null;
            Double cloudCover = null;
            Double irradiance = 0.0; // 단기예보에서 제공하지 않으므로 기본값 0.0

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    String category = item.path("category").asText();
                    double value = item.path("fcstValue").asDouble();

                    switch (category) {
                        case "T1H":
                        case "TMP":
                            temperature = value;
                            break;
                        case "REH":
                            humidity = value;
                            break;
                        case "SKY":
                            // SKY: 1(맑음)~4(흐림)를 0.0~1.0으로 변환
                            cloudCover = (value - 1) / 3.0;
                            break;
                    }
                }
            }

            if (temperature == null || humidity == null || cloudCover == null) {
                throw new BusinessException("기상청 응답에서 필요한 데이터(기온, 습도, 운량)를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST);
            }

            WeatherData weatherData = WeatherData.builder()
                    .powerPlant(plant)
                    .temperature(temperature)
                    .humidity(humidity)
                    .irradiance(irradiance)
                    .cloudCover(cloudCover)
                    .timestamp(LocalDateTime.now())
                    .build();

            log.info("기상 데이터 파싱 완료: {}", weatherData);
            return weatherData;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("기상청 응답 파싱 실패: {}", e.getMessage());
            throw new BusinessException("기상청 응답 데이터를 파싱하는 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}