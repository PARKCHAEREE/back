package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.exception.BusinessException;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.util.CsvParsingUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Tag(name = "기상 데이터 API", description = "과거 기상 데이터 CSV 업로드")
@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
@Slf4j
public class WeatherController {

    private final PowerPlantRepository powerPlantRepository;
    private final PlantFeatureLogRepository plantFeatureLogRepository;
    private final CsvParsingUtil csvParsingUtil;

    @Operation(summary = "과거 기상 데이터 CSV 업로드", description = "과거 기상 데이터가 담긴 CSV 파일을 DB에 저장합니다.")
    @PostMapping(value = "/upload-csv/{plantId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<Map<String, Object>> uploadWeatherData(
            @PathVariable Long plantId,
            @RequestParam("file") MultipartFile file) {

        powerPlantRepository.findById(plantId)
                .orElseThrow(() -> new BusinessException("발전소를 찾을 수 없습니다. ID: " + plantId, HttpStatus.NOT_FOUND));

        List<String[]> csvData = csvParsingUtil.parseCsv(file);
        List<PlantFeatureLog> weatherDataList = new ArrayList<>();
        int successCount = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            try {
                if (row == null || row.length < 34) continue;
                LocalDateTime measuredAt = LocalDateTime.parse(row[0].trim(), formatter);
                PlantFeatureLog data = PlantFeatureLog.builder()
                        .powerPlantId(plantId)
                        .measuredAt(measuredAt)
                        .temp(parseDouble(row[1])).humi(parseDouble(row[2])).clou(parseDouble(row[3]))
                        .wisp(parseDouble(row[4])).hSin(parseDouble(row[5])).hCos(parseDouble(row[6]))
                        .doySin(parseDouble(row[7])).doyCos(parseDouble(row[8])).wideSin(parseDouble(row[9]))
                        .wideCos(parseDouble(row[10])).sunElevClip(parseDouble(row[11])).cosZen(parseDouble(row[12]))
                        .irradiance(parseDouble(row[13])).estIrradiance(parseDouble(row[14]))
                        .irradianceProxy(parseDouble(row[15])).irradianceXCapa(parseDouble(row[16]))
                        .capa(parseDouble(row[17])).seasonalSolarPattern(parseDouble(row[18]))
                        .weatherAdjustedPattern(parseDouble(row[19])).expectedGenProxy(parseDouble(row[20]))
                        .genLag2(parseDouble(row[21])).dustCoverageRatio(parseDouble(row[22]))
                        .snowCoverageRatio(parseDouble(row[23])).birdDroppingCount(parseInteger(row[24]))
                        .physicalDamageCount(parseInteger(row[25])).maxDefectConfidence(parseDouble(row[26]))
                        .clsNormal(parseInteger(row[27])).clsDust(parseInteger(row[28]))
                        .clsSnow(parseInteger(row[29])).clsBird(parseInteger(row[30]))
                        .clsDamage(parseInteger(row[31])).prediction(parseDouble(row[32]))
                        .actual(parseDouble(row[33]))
                        .build();
                weatherDataList.add(data);
                successCount++;
            } catch (Exception e) {
                log.warn("{}번째 줄 변환 중 오류 발생 (건너뜀): {}", i + 1, e.getMessage());
            }
        }
        plantFeatureLogRepository.saveAll(weatherDataList);
        log.info("기상 데이터 CSV 적재 완료! 발전소 ID: {}, 총 {}건 저장", plantId, successCount);
        return ResponseEntity.ok(Map.of("totalRows", csvData.size() - 1, "successCount", successCount));
    }

    private double parseDouble(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return 0.0;
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) { return 0.0; }
    }

    private int parseInteger(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return 0;
            return (int) Math.round(Double.parseDouble(value.trim()));
        } catch (NumberFormatException e) { return 0; }
    }
}
