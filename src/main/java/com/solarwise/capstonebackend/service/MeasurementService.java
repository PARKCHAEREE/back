package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.DashboardSummaryDto;
import com.solarwise.capstonebackend.dto.MeasurementCsvUploadResult;
import com.solarwise.capstonebackend.dto.MeasurementDto;
import com.solarwise.capstonebackend.dto.MeasurementSeriesDto;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.util.CsvParsingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 측정 데이터 서비스
 * - 시계열 발전량 데이터 조회 및 대시보드 요약
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeasurementService {

    private static final DateTimeFormatter MEASUREMENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private final PlantFeatureLogRepository plantFeatureLogRepository;
    private final PowerPlantRepository powerPlantRepository;
    private final AnomalyRepository anomalyRepository;
    private final CsvParsingUtil csvParsingUtil;
    private final SimulationService simulationService;

    /**
     * 실측 CSV 업로드
     */
    @Transactional
    public MeasurementCsvUploadResult uploadMeasurementCsv(Long plantId, Long userId, MultipartFile file) {
        PowerPlant plant = powerPlantRepository.findByIdAndUserId(plantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        List<String[]> rows = csvParsingUtil.parseCsv(file);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV 파일이 비어 있습니다.");
        }
        if (rows.size() == 1) {
            throw new IllegalArgumentException("업로드할 실측 데이터가 없습니다.");
        }

        Map<String, Integer> headerIndex = buildHeaderIndex(rows.getFirst());
        validateRequiredHeaders(headerIndex);

        List<ParsedMeasurementRow> parsedRows = rows.stream()
                .skip(1)
                .map(row -> parseMeasurementRow(row, headerIndex))
                .filter(Objects::nonNull)
                .toList();

        int totalRows = rows.size() - 1;
        int skippedRows = totalRows - parsedRows.size();

        if (parsedRows.isEmpty()) {
            throw new IllegalArgumentException("저장 가능한 실측 데이터가 없습니다.");
        }

        // 앞뒤 값을 기반으로 온도·습도 누락값 선형 보간
        List<ParsedMeasurementRow> interpolatedRows = interpolateMissingValues(parsedRows);

        LocalDateTime firstTimestamp = interpolatedRows.get(0).timestamp();
        LocalDateTime lastTimestamp = interpolatedRows.get(interpolatedRows.size() - 1).timestamp();

        Set<LocalDateTime> existingTimestamps = plantFeatureLogRepository
                .findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(plantId, firstTimestamp, lastTimestamp)
                .stream()
                .map(PlantFeatureLog::getMeasuredAt)
                .collect(Collectors.toCollection(HashSet::new));

        List<PlantFeatureLog> logsToSave = interpolatedRows.stream()
                .filter(parsed -> existingTimestamps.add(parsed.timestamp()))
                .map(parsed -> PlantFeatureLog.builder()
                        .powerPlantId(plant.getId())
                        .measuredAt(parsed.timestamp())
                        .actual(parsed.energyKwh())
                        .prediction(0.0) // CSV에 예측값 없음
                        .temp(parsed.temperature())
                        .humi(parsed.humidity())
                        .clou(0.0) // CSV에 운량 없음
                        .irradiance(null) // CSV에 일사량 없음
                        .wisp(0.0) // 풍속 기본값
                        .build())
                .toList();

        plantFeatureLogRepository.saveAll(logsToSave);

        int duplicateRows = interpolatedRows.size() - logsToSave.size();
        log.info("실측 CSV 업로드 완료: plantId={}, total={}, saved={}, duplicate={}, skipped={}",
                plant.getId(), totalRows, logsToSave.size(), duplicateRows, skippedRows);

        return MeasurementCsvUploadResult.builder()
                .plantId(plant.getId())
                .fileName(file.getOriginalFilename())
                .totalRows(totalRows)
                .parsedRows(interpolatedRows.size())
                .savedRows(logsToSave.size())
                .duplicateRows(duplicateRows)
                .skippedRows(skippedRows)
                .firstTimestamp(firstTimestamp)
                .lastTimestamp(lastTimestamp)
                .build();
    }

    /**
     * 대시보드 요약 조회
     */
    public DashboardSummaryDto getDashboardSummary(Long plantId, Long userId) {
        PowerPlant plant = powerPlantRepository.findByIdAndUserId(plantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        // 현재 발전량 (가장 최근 데이터)
        PlantFeatureLog latestLog = plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtDesc(plant.getId());

        Double currentPowerKw = latestLog != null ? latestLog.getActual() : 0.0;

        // ✅ FIXED: 가상 시간 기준으로 "금일" 범위 설정
        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        LocalDateTime todayStart = LocalDateTime.of(virtualNow.toLocalDate(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(virtualNow.toLocalDate(), LocalTime.MAX);
        List<PlantFeatureLog> todayLogs = plantFeatureLogRepository
                .findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(plant.getId(), todayStart, todayEnd);

        Double todayGenerationKwh = todayLogs.stream()
                .mapToDouble(log -> log.getActual() != null ? log.getActual() : 0.0)
                .sum();

        // 효율 (향후 확장)
        Double efficiencyPercent = 87.1; // TODO: 실제 효율 계산

        // 최근 이상 정보
        List<Anomaly> recentAnomalies = anomalyRepository
                .findByPowerPlantIdOrderByDetectedAtDesc(plant.getId());

        DashboardSummaryDto.AnomalyInfo anomalyInfo;
        if (!recentAnomalies.isEmpty()) {
            Anomaly latest = recentAnomalies.getFirst();
            anomalyInfo = DashboardSummaryDto.AnomalyInfo.builder()
                    .exists(true)
                    .eventId(latest.getId())
                    .severity(latest.getSeverity())
                    .summary(latest.getSummary())
                    .build();
        } else {
            anomalyInfo = DashboardSummaryDto.AnomalyInfo.builder()
                    .exists(false)
                    .build();
        }

        // ✅ FIXED: lastUpdatedAt도 가상 시간 기반 설정
        return DashboardSummaryDto.builder()
                .currentPowerKw(currentPowerKw)
                .todayGenerationKwh(todayGenerationKwh)
                .efficiencyPercent(efficiencyPercent)
                .lastUpdatedAt(latestLog != null ? latestLog.getMeasuredAt() : virtualNow)
                .latestAnomaly(anomalyInfo)
                .build();
    }

    /**
     * 시계열 측정 데이터 조회
     */
    public MeasurementSeriesDto getMeasurementSeries(Long plantId, Long userId,
                                                      LocalDateTime from, LocalDateTime to) {
        PowerPlant plant = powerPlantRepository.findByIdAndUserId(plantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        List<PlantFeatureLog> measurements = plantFeatureLogRepository
                .findByPowerPlantIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(plant.getId(), from, to);

        List<MeasurementDto> series = measurements.stream()
                .map(log -> MeasurementDto.builder()
                        .measuredAt(log.getMeasuredAt())
                        .powerKw(log.getActual())
                        .energyKwh(log.getActual())
                        .temperature(log.getTemp())
                        .irradiance(log.getIrradiance())
                        .humidity(log.getHumi())
                        .build())
                .collect(Collectors.toList());

        return MeasurementSeriesDto.builder()
                .plantId(plant.getId())
                .series(series)
                .build();
    }

    private Map<String, Integer> buildHeaderIndex(String[] headerRow) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int i = 0; i < headerRow.length; i++) {
            headerIndex.put(headerRow[i].trim(), i);
        }
        return headerIndex;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        List<String> requiredHeaders = List.of("TIME", "D_PERIOD_GEN_KWH", "D_TEMP", "D_HUMIDITY");
        List<String> missingHeaders = requiredHeaders.stream()
                .filter(header -> !headerIndex.containsKey(header))
                .toList();

        if (!missingHeaders.isEmpty()) {
            throw new IllegalArgumentException("필수 CSV 헤더가 누락되었습니다: " + String.join(", ", missingHeaders));
        }
    }

    private ParsedMeasurementRow parseMeasurementRow(String[] row, Map<String, Integer> headerIndex) {
        try {
            LocalDateTime timestamp = LocalDateTime.parse(
                    readRequiredValue(row, headerIndex, "TIME"), MEASUREMENT_TIME_FORMATTER);
            double energyKwh = Double.parseDouble(readRequiredValue(row, headerIndex, "D_PERIOD_GEN_KWH"));
            // 온도·습도는 선택값 — null 유지 후 보간 처리
            Double temperature = parseNullableDouble(readOptionalValue(row, headerIndex, "D_TEMP"));
            Double humidity    = parseNullableDouble(readOptionalValue(row, headerIndex, "D_HUMIDITY"));

            return new ParsedMeasurementRow(timestamp, energyKwh, temperature, humidity);
        } catch (Exception e) {
            log.warn("실측 CSV 행 파싱 실패 - 건너뜀: {}", e.getMessage());
            return null;
        }
    }

    private String readRequiredValue(String[] row, Map<String, Integer> headerIndex, String header) {
        String value = readOptionalValue(row, headerIndex, header);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(header + " 값이 비어 있습니다.");
        }
        return value;
    }

    private String readOptionalValue(String[] row, Map<String, Integer> headerIndex, String header) {
        Integer index = headerIndex.get(header);
        if (index == null || index >= row.length) {
            return null;
        }
        return row[index].trim();
    }

    private double parseOptionalDouble(String value) {
        return value == null || value.isBlank() ? 0.0 : Double.parseDouble(value);
    }

    /** 빈 문자열이면 null 반환 (보간 대상 식별용) */
    private Double parseNullableDouble(String value) {
        if (value == null || value.isBlank()) return null;
        return Double.parseDouble(value);
    }

    /**
     * 온도·습도의 누락값을 앞뒤 데이터 기반 선형 보간으로 채웁니다.
     * <ul>
     *   <li>앞·뒤 모두 존재하면 선형 보간 (weighted average)</li>
     *   <li>앞만 있으면 앞 값으로 채움 (backward fill)</li>
     *   <li>뒤만 있으면 뒤 값으로 채움 (forward fill)</li>
     *   <li>전체가 null이면 null 유지</li>
     * </ul>
     */
    private List<ParsedMeasurementRow> interpolateMissingValues(List<ParsedMeasurementRow> rows) {
        List<Double> temps     = rows.stream().map(ParsedMeasurementRow::temperature).toList();
        List<Double> humidities = rows.stream().map(ParsedMeasurementRow::humidity).toList();

        List<Double> filledTemps     = linearInterpolate(temps);
        List<Double> filledHumidities = linearInterpolate(humidities);

        List<ParsedMeasurementRow> result = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            ParsedMeasurementRow original = rows.get(i);
            result.add(new ParsedMeasurementRow(
                    original.timestamp(),
                    original.energyKwh(),
                    filledTemps.get(i),
                    filledHumidities.get(i)
            ));
        }
        return result;
    }

    private List<Double> linearInterpolate(List<Double> values) {
        int n = values.size();
        List<Double> result = new ArrayList<>(values);

        for (int i = 0; i < n; i++) {
            if (result.get(i) != null) continue;

            // 왼쪽 비결측 인덱스 탐색
            int prevIdx = -1;
            for (int j = i - 1; j >= 0; j--) {
                if (result.get(j) != null) { prevIdx = j; break; }
            }
            // 오른쪽 비결측 인덱스 탐색
            int nextIdx = -1;
            for (int j = i + 1; j < n; j++) {
                if (result.get(j) != null) { nextIdx = j; break; }
            }

            if (prevIdx >= 0 && nextIdx >= 0) {
                double ratio = (double) (i - prevIdx) / (nextIdx - prevIdx);
                result.set(i, result.get(prevIdx) + ratio * (result.get(nextIdx) - result.get(prevIdx)));
            } else if (prevIdx >= 0) {
                result.set(i, result.get(prevIdx));  // backward fill
            } else if (nextIdx >= 0) {
                result.set(i, result.get(nextIdx));  // forward fill
            }
            // 전체 null이면 그대로 null 유지
        }
        return result;
    }

    private record ParsedMeasurementRow(
            LocalDateTime timestamp,
            Double energyKwh,
            Double temperature,
            Double humidity
    ) {
    }

}
