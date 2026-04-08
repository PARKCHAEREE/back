package com.solarwise.capstonebackend.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
}