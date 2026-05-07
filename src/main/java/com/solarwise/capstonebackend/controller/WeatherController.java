package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.entity.WeatherData;
import com.solarwise.capstonebackend.service.AiIntegrationService;
import com.solarwise.capstonebackend.service.WeatherDataImportService;
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
    private final WeatherDataImportService weatherDataImportService;

    // 1. 실시간 날씨 조회
    @Operation(summary = "실시간 동네 날씨 조회")
    @GetMapping("/weather/current")
    public ResponseEntity<ApiResponse<Map<String, Double>>> getCurrentWeather(
            @RequestParam Long plantId) {

        // 시뮬레이션 모드: 고정 기상 데이터 반환
        Map<String, Double> weatherMap = Map.of(
                "temperature", 25.0,
                "humidity", 60.0,
                "irradiance", 800.0,
                "cloudCover", 0.3
        );

        // 공통 응답 포맷으로 감싸서 리턴
        return ResponseEntity.ok(ApiResponse.success(weatherMap, "실시간 기상 데이터 조회 성공"));
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

    // 3. 우양 제공 태양광/기상 데이터 CSV 업로드 (EnergyLog 적재)
    @Operation(summary = "우양 제공 태양광/기상 데이터 CSV 업로드 및 EnergyLog 적재")
    @PostMapping(value = "/plants/{plantId}/weather/upload-advisor-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAdvisorDataCsv(
            @PathVariable Long plantId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean enableDemoCheat) {

        Map<String, Object> result = weatherDataImportService.importAdvisorCsvToEnergyLog(plantId, file, enableDemoCheat);
        return ResponseEntity.ok(ApiResponse.success(result, "우양 CSV 데이터 적재 완료"));
    }
}