package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.service.WeatherDataImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "Simulation Setup API", description = "발전소 시뮬레이션 초기 데이터 세팅용 (관리자 전용)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherDataImportService weatherDataImportService;

    /**
     * 우양 제공 태양광/기상 데이터 통합 CSV 업로드
     * PlantFeatureLog 테이블에 8개월치 데이터를 초기 세팅합니다.
     */
    @Operation(summary = "초기 시뮬레이션 데이터 통합 CSV 업로드 및 세팅 (PlantFeatureLog 적재)")
    @PostMapping(value = "/plants/{plantId}/weather/upload-advisor-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAdvisorDataCsv(
            @PathVariable Long plantId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean enableDemoCheat) {
        
        Map<String, Object> result = weatherDataImportService.importAdvisorCsvToEnergyLog(plantId, file, enableDemoCheat);
        return ResponseEntity.ok(ApiResponse.success(result, "시뮬레이션 초기 데이터 적재 완료"));
    }
}