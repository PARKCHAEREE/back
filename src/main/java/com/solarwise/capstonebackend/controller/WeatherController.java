package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.service.AiIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "Weather API", description = "기상청 실시간 예보 및 과거 CSV 데이터 적재")
@RestController
@RequestMapping("/api/v1") // 공통 v1 경로 적용
@RequiredArgsConstructor
public class WeatherController {

    private final AiIntegrationService aiIntegrationService;

    // 1. 실시간 날씨 조회
    @Operation(summary = "실시간 동네 날씨 조회")
    @GetMapping("/weather/current")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getCurrentWeather(
            @RequestParam(defaultValue = "60") int nx,
            @RequestParam(defaultValue = "127") int ny) {

        Map<String, Double> weatherData = aiIntegrationService.fetchRealTimeWeather(nx, ny);

        // 공통 응답 포맷으로 감싸서 리턴
        return ResponseEntity.ok(ApiResponse.success(weatherData, "실시간 기상 데이터 조회 성공"));
    }

    // 2. CSV 파일 업로드 
    @Operation(summary = "과거 기상 데이터 CSV 업로드")
    @PostMapping(value = "/plants/{plantId}/weather/upload-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadWeatherCsv(
            @PathVariable Long plantId,
            @RequestPart("file") MultipartFile file) {

        Map<String, Object> result = aiIntegrationService.uploadWeatherDataCsv(plantId, file);
        return ResponseEntity.ok(ApiResponse.success(result, "CSV 업로드 완료"));
    }
}