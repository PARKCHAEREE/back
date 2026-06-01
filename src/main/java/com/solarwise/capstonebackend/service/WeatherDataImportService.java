package com.solarwise.capstonebackend.service;

import com.opencsv.bean.CsvToBeanBuilder;
import com.solarwise.capstonebackend.dto.AdvisorDataCsvDto;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.exception.BusinessException;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CSV 데이터 임포트 서비스
 * - 우양 제공 31개 피처 CSV 파일을 파싱하여 PlantFeatureLog 엔티티로 변환하여 DB 적재
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherDataImportService {

    private final PlantFeatureLogRepository plantFeatureLogRepository;
    private final PowerPlantRepository powerPlantRepository;

    @Transactional
    public Map<String, Object> importAdvisorCsvToEnergyLog(Long powerPlantId, MultipartFile file, boolean enableDemoCheat) {
        if (file.isEmpty()) {
            throw new BusinessException("파일이 비어있습니다.", HttpStatus.BAD_REQUEST);
        }

        PowerPlant powerPlant = powerPlantRepository.findById(powerPlantId)
                .orElseThrow(() -> new BusinessException("발전소를 찾을 수 없습니다. ID: " + powerPlantId, HttpStatus.NOT_FOUND));

        List<AdvisorDataCsvDto> csvDataList;
        try {
            csvDataList = parseCsvFile(file);
        } catch (Exception e) {
            log.error("CSV 파일 파싱 실패: {}", e.getMessage());
            throw new BusinessException("CSV 파일 파싱 중 오류가 발생했습니다: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        List<PlantFeatureLog> featureLogList = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (AdvisorDataCsvDto csvDto : csvDataList) {
            try {
                String rawTime = csvDto.getTime().trim();
                LocalDateTime timestamp = LocalDateTime.parse(rawTime, formatter);

                // 캡스톤 시연용 가짜 이상 탐지 조작
                Double actualValue = csvDto.getActual() != null ? csvDto.getActual() : 0.0;
                if (enableDemoCheat && timestamp.isAfter(LocalDateTime.of(2026, 3, 15, 13, 0))) {
                    actualValue *= 0.4;
                    log.debug("데모 조작 적용: {} → {} (40%로 감소)", csvDto.getActual(), actualValue);
                }

                // PlantFeatureLog 엔티티 빌드 (31개 피처 매핑)
                PlantFeatureLog featureLog = PlantFeatureLog.builder()
                        .powerPlantId(powerPlantId)
                        .measuredAt(timestamp)
                        // 1. 기본 발전량 데이터
                        .actual(actualValue)
                        .prediction(csvDto.getPrediction() != null ? csvDto.getPrediction() : 0.0)
                        // 2. 기상/환경 변수
                        .temp(csvDto.getTemp() != null ? csvDto.getTemp() : 0.0)
                        .humi(csvDto.getHumi() != null ? csvDto.getHumi() : 0.0)
                        .clou(csvDto.getClou() != null ? csvDto.getClou() : 0.0)
                        .wisp(csvDto.getWisp() != null ? csvDto.getWisp() : 0.0)
                        // 3. 시간 및 계절성 파생 변수
                        .hSin(csvDto.getHSin() != null ? csvDto.getHSin() : 0.0)
                        .hCos(csvDto.getHCos() != null ? csvDto.getHCos() : 0.0)
                        .doySin(csvDto.getDoySin() != null ? csvDto.getDoySin() : 0.0)
                        .doyCos(csvDto.getDoyCos() != null ? csvDto.getDoyCos() : 0.0)
                        .wideSin(csvDto.getWideSin() != null ? csvDto.getWideSin() : 0.0)
                        .wideCos(csvDto.getWideCos() != null ? csvDto.getWideCos() : 0.0)
                        // 4. 일사량 및 발전소 용량 변수
                        .sunElevClip(csvDto.getSunElevClip() != null ? csvDto.getSunElevClip() : 0.0)
                        .cosZen(csvDto.getCosZen() != null ? csvDto.getCosZen() : 0.0)
                        .irradiance(csvDto.getIrradiance() != null ? csvDto.getIrradiance() : 0.0)
                        .estIrradiance(csvDto.getEstIrradiance() != null ? csvDto.getEstIrradiance() : 0.0)
                        .irradianceProxy(csvDto.getIrradianceProxy() != null ? csvDto.getIrradianceProxy() : 0.0)
                        .irradianceXCapa(csvDto.getIrradianceXCapa() != null ? csvDto.getIrradianceXCapa() : 0.0)
                        .capa(csvDto.getCapa() != null ? csvDto.getCapa() : 0.0)
                        // 5. 발전량 패턴 및 과거 시계열
                        .seasonalSolarPattern(csvDto.getSeasonalSolarPattern() != null ? csvDto.getSeasonalSolarPattern() : 0.0)
                        .weatherAdjustedPattern(csvDto.getWeatherAdjustedPattern() != null ? csvDto.getWeatherAdjustedPattern() : 0.0)
                        .expectedGenProxy(csvDto.getExpectedGenProxy() != null ? csvDto.getExpectedGenProxy() : 0.0)
                        .genLag2(csvDto.getGenLag2() != null ? csvDto.getGenLag2() : 0.0)
                        // 6. 이상 탐지 관련 신규 피처
                        .dustCoverageRatio(csvDto.getDustCoverageRatio() != null ? csvDto.getDustCoverageRatio() : 0.0)
                        .snowCoverageRatio(csvDto.getSnowCoverageRatio() != null ? csvDto.getSnowCoverageRatio() : 0.0)
                        .birdDroppingCount(csvDto.getBirdDroppingCount() != null ? csvDto.getBirdDroppingCount() : 0)
                        .physicalDamageCount(csvDto.getPhysicalDamageCount() != null ? csvDto.getPhysicalDamageCount() : 0)
                        .maxDefectConfidence(csvDto.getMaxDefectConfidence() != null ? csvDto.getMaxDefectConfidence() : 0.0)
                        .clsNormal(csvDto.getClsNormal() != null ? csvDto.getClsNormal() : 0)
                        .clsDust(csvDto.getClsDust() != null ? csvDto.getClsDust() : 0)
                        .clsSnow(csvDto.getClsSnow() != null ? csvDto.getClsSnow() : 0)
                        .clsBird(csvDto.getClsBird() != null ? csvDto.getClsBird() : 0)
                        .clsDamage(csvDto.getClsDamage() != null ? csvDto.getClsDamage() : 0)
                        .build();

                featureLogList.add(featureLog);
                successCount++;

            } catch (DateTimeParseException e) {
                log.warn("시간 포맷 파싱 실패 - TIME: {}, 오류: {}", csvDto.getTime(), e.getMessage());
                failureCount++;
            } catch (NumberFormatException e) {
                log.warn("숫자 포맷 파싱 실패 - 데이터: {}, 오류: {}", csvDto, e.getMessage());
                failureCount++;
            } catch (Exception e) {
                log.warn("행 처리 중 알 수 없는 에러 - 데이터: {}, 오류: {}", csvDto, e.getMessage());
                failureCount++;
            }
        }

        if (!featureLogList.isEmpty()) {
            plantFeatureLogRepository.deleteByPowerPlantId(powerPlantId);
            plantFeatureLogRepository.saveAll(featureLogList);
            log.info("PlantFeatureLog 적재 완료! 발전소 ID: {}, 저장: {}건, 실패: {}건", powerPlantId, featureLogList.size(), failureCount);
        } else {
            log.warn("적재할 유효한 데이터가 없습니다. 발전소 ID: {}", powerPlantId);
        }

        return Map.of(
                "totalRows", csvDataList.size(),
                "successCount", successCount,
                "failureCount", failureCount
        );
    }

    private List<AdvisorDataCsvDto> parseCsvFile(MultipartFile file) throws Exception {
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"))) {
            List<AdvisorDataCsvDto> csvDataList = new CsvToBeanBuilder<AdvisorDataCsvDto>(reader)
                    .withType(AdvisorDataCsvDto.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build()
                    .parse();

            if (csvDataList.isEmpty()) {
                throw new BusinessException("CSV 파일에 데이터가 없습니다.", HttpStatus.BAD_REQUEST);
            }

            return csvDataList;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("CSV 파일 파싱 실패: {}", e.getMessage());
            throw new BusinessException("CSV 파일 파싱 중 오류가 발생했습니다.", HttpStatus.BAD_REQUEST);
        }
    }
}