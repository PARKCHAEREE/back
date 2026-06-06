package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.MeasurementCsvUploadResult;
import com.solarwise.capstonebackend.dto.MeasurementDto;
import com.solarwise.capstonebackend.dto.MeasurementSeriesDto;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.util.CsvParsingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasurementService {

    private static final DateTimeFormatter MEASUREMENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private final PlantFeatureLogRepository plantFeatureLogRepository;
    private final PowerPlantRepository powerPlantRepository;
    private final CsvParsingUtil csvParsingUtil;
    private final SimulationService simulationService;

    @Transactional
    public MeasurementCsvUploadResult uploadMeasurementCsv(Long plantId, Long userId, MultipartFile file) {
        PowerPlant plant = powerPlantRepository.findByIdAndUserId(plantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        List<String[]> rows = csvParsingUtil.parseCsv(file);
        if (rows.size() <= 1) throw new IllegalArgumentException("업로드할 실측 데이터가 없습니다.");

        Map<String, Integer> headerIndex = buildHeaderIndex(rows.get(0));
        validateRequiredHeaders(headerIndex);

        List<ParsedMeasurementRow> parsedRows = rows.stream().skip(1)
                .map(row -> parseMeasurementRow(row, headerIndex))
                .filter(Objects::nonNull).toList();

        List<ParsedMeasurementRow> interpolatedRows = interpolateMissingValues(parsedRows);
        LocalDateTime firstTimestamp = interpolatedRows.get(0).timestamp();
        LocalDateTime lastTimestamp = interpolatedRows.get(interpolatedRows.size() - 1).timestamp();

        Map<LocalDateTime, PlantFeatureLog> existingLogsMap = plantFeatureLogRepository
                .findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(plantId, firstTimestamp, lastTimestamp)
                .stream().collect(Collectors.toMap(PlantFeatureLog::getMeasuredAt, log -> log));

        List<PlantFeatureLog> logsToSave = new ArrayList<>();
        for (ParsedMeasurementRow parsed : interpolatedRows) {
            PlantFeatureLog log = existingLogsMap.getOrDefault(parsed.timestamp(), PlantFeatureLog.builder()
                    .powerPlantId(plant.getId()).measuredAt(parsed.timestamp()).build());
            log.setActual(parsed.energyKwh());
            log.setTemp(parsed.temperature());
            log.setHumi(parsed.humidity());
            logsToSave.add(log);
        }

        plantFeatureLogRepository.saveAll(logsToSave);
        return MeasurementCsvUploadResult.builder().plantId(plant.getId()).fileName(file.getOriginalFilename())
                .totalRows(rows.size() - 1).savedRows(logsToSave.size()).build();
    }

    public MeasurementSeriesDto getMeasurementSeries(Long plantId, Long userId, LocalDateTime from, LocalDateTime to) {
        log.info("조회 시도 중인 유저 ID: {}", userId);
        powerPlantRepository.findByIdAndUserId(plantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        // 💡 최종 수정: 프론트엔드의 시간 값을 무시하고, 오직 백엔드의 가상 시간만 사용
        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        LocalDateTime endTime = virtualNow;
        LocalDateTime startTime = endTime.minusHours(24);

        log.info("대시보드 데이터 조회 범위: {} ~ {}", startTime, endTime);

        List<PlantFeatureLog> measurements = plantFeatureLogRepository
                .findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(plantId, startTime, endTime);

        List<MeasurementDto> series = measurements.stream()
                .map(log -> MeasurementDto.builder()
                        .measuredAt(log.getMeasuredAt())
                        .powerKw(log.getActual())
                        .temperature(log.getTemp())
                        .irradiance(log.getIrradiance())
                        .humidity(log.getHumi())
                        .build())
                .collect(Collectors.toList());

        return MeasurementSeriesDto.builder().plantId(plantId).series(series).build();
    }

    private Map<String, Integer> buildHeaderIndex(String[] headerRow) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int i = 0; i < headerRow.length; i++) headerIndex.put(headerRow[i].trim(), i);
        return headerIndex;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        if (!headerIndex.containsKey("TIME") || !headerIndex.containsKey("D_PERIOD_GEN_KWH")) {
            throw new IllegalArgumentException("필수 CSV 헤더(TIME, D_PERIOD_GEN_KWH)가 누락되었습니다.");
        }
    }

    private ParsedMeasurementRow parseMeasurementRow(String[] row, Map<String, Integer> headerIndex) {
        try {
            LocalDateTime timestamp = LocalDateTime.parse(row[headerIndex.get("TIME")], MEASUREMENT_TIME_FORMATTER);
            double energyKwh = Double.parseDouble(row[headerIndex.get("D_PERIOD_GEN_KWH")]);
            Double temperature = parseNullableDouble(readOptionalValue(row, headerIndex, "D_TEMP"));
            Double humidity = parseNullableDouble(readOptionalValue(row, headerIndex, "D_HUMIDITY"));
            return new ParsedMeasurementRow(timestamp, energyKwh, temperature, humidity);
        } catch (Exception e) {
            log.warn("실측 CSV 행 파싱 실패 - 건너뜀: {}", String.join(",", row));
            return null;
        }
    }

    private String readOptionalValue(String[] row, Map<String, Integer> headerIndex, String header) {
        Integer index = headerIndex.get(header);
        return (index != null && index < row.length) ? row[index].trim() : null;
    }

    private Double parseNullableDouble(String value) {
        return (value == null || value.isBlank()) ? null : Double.parseDouble(value);
    }

    private List<ParsedMeasurementRow> interpolateMissingValues(List<ParsedMeasurementRow> rows) {
        List<Double> temps = rows.stream().map(ParsedMeasurementRow::temperature).collect(Collectors.toList());
        List<Double> humidities = rows.stream().map(ParsedMeasurementRow::humidity).collect(Collectors.toList());
        List<Double> filledTemps = linearInterpolate(temps);
        List<Double> filledHumidities = linearInterpolate(humidities);
        List<ParsedMeasurementRow> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            ParsedMeasurementRow original = rows.get(i);
            result.add(new ParsedMeasurementRow(original.timestamp, original.energyKwh, filledTemps.get(i), filledHumidities.get(i)));
        }
        return result;
    }

    private List<Double> linearInterpolate(List<Double> values) {
        int n = values.size();
        List<Double> result = new ArrayList<>(values);
        for (int i = 0; i < n; i++) {
            if (result.get(i) != null) continue;
            int prevIdx = -1, nextIdx = -1;
            for (int j = i - 1; j >= 0; j--) if (result.get(j) != null) { prevIdx = j; break; }
            for (int j = i + 1; j < n; j++) if (result.get(j) != null) { nextIdx = j; break; }
            if (prevIdx != -1 && nextIdx != -1) {
                double ratio = (double) (i - prevIdx) / (nextIdx - prevIdx);
                result.set(i, result.get(prevIdx) + ratio * (result.get(nextIdx) - result.get(prevIdx)));
            } else if (prevIdx != -1) result.set(i, result.get(prevIdx));
            else if (nextIdx != -1) result.set(i, result.get(nextIdx));
        }
        return result;
    }

    private record ParsedMeasurementRow(LocalDateTime timestamp, Double energyKwh, Double temperature, Double humidity) {}
}
