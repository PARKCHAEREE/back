package com.solarwise.capstonebackend.service;

import com.opencsv.bean.CsvToBeanBuilder;
import com.solarwise.capstonebackend.dto.AdvisorDataCsvDto;
import com.solarwise.capstonebackend.entity.EnergyLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.entity.WeatherData;
import com.solarwise.capstonebackend.exception.BusinessException;
import com.solarwise.capstonebackend.repository.EnergyLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.repository.WeatherDataRepository;
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
 * 기상 데이터 임포트 서비스
 * - 우양 제공 CSV 파일 파싱 및 EnergyLog 엔티티로 변환하여 DB 적재
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherDataImportService {

    private final EnergyLogRepository energyLogRepository;
    private final PowerPlantRepository powerPlantRepository;
    private final WeatherDataRepository weatherDataRepository;

    /**
     * 우양 제공 태양광/기상 데이터 CSV를 파싱하여 EnergyLog와 WeatherData로 적재합니다.
     *
     * @param powerPlantId 데이터를 적재할 발전소의 ID
     * @param file 업로드된 CSV 파일
     * @param enableDemoCheat 캡스톤 시연용 가짜 이상 탐지 조작 활성화 여부
     * @return 성공 및 실패 건수를 포함한 처리 결과 Map
     * @throws BusinessException CSV 파싱 실패 또는 발전소를 찾을 수 없을 경우
     */
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

        List<EnergyLog> energyLogList = new ArrayList<>();
        List<WeatherData> weatherDataList = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (AdvisorDataCsvDto csvDto : csvDataList) {
            try {
                String rawTime = csvDto.getTime().trim();
                LocalDateTime timestamp = LocalDateTime.parse(rawTime, formatter);

                // 캡스톤 시연용 가짜 이상 탐지 조작: 10월 2일 오후 1시(13시) 이후 실제 발전량을 40%로 깎음
                Double powerKw = csvDto.getActual() != null ? csvDto.getActual() : 0.0;
                if (enableDemoCheat && timestamp.isAfter(LocalDateTime.of(2026, 3, 15, 13, 0))) {
                    powerKw *= 0.4; // 13시 이후부터 이상 발생 시작!
                }

                // EnergyLog 엔티티 빌드
                EnergyLog energyLog = EnergyLog.builder()
                        .powerPlant(powerPlant)
                        .powerKw(powerKw)
                        .temperature(csvDto.getTemp() != null ? csvDto.getTemp() : 0.0)
                        .humidity(csvDto.getHumi() != null ? csvDto.getHumi() : 0.0)
                        .irradiance(csvDto.getIrradiance() != null ? csvDto.getIrradiance() : 0.0)
                        .timestamp(timestamp)
                        .build();

                energyLogList.add(energyLog);

                // WeatherData 엔티티 빌드
                WeatherData weatherData = WeatherData.builder()
                        .powerPlant(powerPlant)
                        .temperature(csvDto.getTemp() != null ? csvDto.getTemp() : 0.0)
                        .humidity(csvDto.getHumi() != null ? csvDto.getHumi() : 0.0)
                        .irradiance(csvDto.getIrradiance() != null ? csvDto.getIrradiance() : 0.0)
                        .cloudCover(csvDto.getClou() != null ? csvDto.getClou() / 100.0 : 0.0)
                        .timestamp(timestamp)
                        .build();

                weatherDataList.add(weatherData);
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

        // 성공한 데이터만 DB에 적재
        if (!energyLogList.isEmpty()) {
            energyLogRepository.saveAll(energyLogList);
            weatherDataRepository.saveAll(weatherDataList);
            log.info("데이터 적재 완료! 발전소 ID: {}, EnergyLog: {}건, WeatherData: {}건, 실패: {}건", powerPlantId, energyLogList.size(), weatherDataList.size(), failureCount);
        } else {
            log.warn("적재할 유효한 데이터가 없습니다. 발전소 ID: {}", powerPlantId);
        }

        return Map.of(
                "totalRows", csvDataList.size(),
                "successCount", successCount,
                "failureCount", failureCount
        );
    }

    /**
     * CSV 파일을 파싱하여 AdvisorDataCsvDto 리스트로 변환합니다.
     * OpenCSV 라이브러리를 사용하여 @CsvBindByName 어노테이션 기반의 자동 매핑을 수행합니다.
     *
     * @param file 업로드된 CSV 파일
     * @return 파싱된 DTO 리스트
     * @throws Exception 파일 읽기 또는 파싱 실패 시
     */
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

            log.info("CSV 파일 파싱 성공: {}건의 행 읽음", csvDataList.size());
            return csvDataList;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("CSV 파일 파싱 실패: {}", e.getMessage());
            throw new BusinessException("CSV 파일 파싱 중 오류가 발생했습니다.", HttpStatus.BAD_REQUEST);
        }
    }

}
