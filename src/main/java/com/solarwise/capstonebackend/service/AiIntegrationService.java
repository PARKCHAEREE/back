package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.entity.EnergyLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.entity.WeatherData;
import com.solarwise.capstonebackend.repository.EnergyLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.repository.WeatherDataRepository;
import com.solarwise.capstonebackend.util.CsvParsingUtil;
import com.solarwise.capstonebackend.util.WeatherDataFormatterUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI 서버 통신 서비스
 * - Python/PyTorch AI 서버와 비동기 통신
 * - 발전량 예측, 이상 탐지 결과 수신 및 DB 갱신
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiIntegrationService {

    private final RestTemplate restTemplate;
    private final PowerPlantRepository powerPlantRepository;
    private final EnergyLogRepository energyLogRepository;
    private final WeatherDataFormatterUtil weatherDataFormatterUtil;

    private final CsvParsingUtil csvParsingUtil;
    private final WeatherDataRepository weatherDataRepository;

    @Value("${kma.api.key}")
    private String apiKey;

    // 기상청 API( 실시간 날씨 데이터 수집)
    public Map<String, Double> fetchRealTimeWeather(int nx, int ny) {
        String baseDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = "0500"; // 단기예보 기준 시간

        String url = String.format(
                "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst?serviceKey=%s&pageNo=1&numOfRows=10&dataType=JSON&base_date=%s&base_time=%s&nx=%d&ny=%d",
                apiKey, baseDate, baseTime, nx, ny
        );

        log.info("기상청 API 호출: (X:{}, Y:{})", nx, ny);

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            return weatherDataFormatterUtil.formatWeatherData(jsonResponse);
        } catch (Exception e) {
            log.error("기상청 API 호출 실패: {}", e.getMessage());
            throw new RuntimeException("기상청 통신 오류", e);
        }
    }
    // --- [2] CSV 과거 기상 데이터 업로드 및 DB 적재 (새로 추가된 로직!) ---
    @Transactional
    public Map<String, Object> uploadWeatherDataCsv(Long powerPlantId, MultipartFile file) {
        // 1. 데이터가 들어갈 발전소 엔티티 조회
        PowerPlant powerPlant = powerPlantRepository.findById(powerPlantId)
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + powerPlantId));

        // 2. OpenCSV를 통해 파일 파싱
        List<String[]> csvData = csvParsingUtil.parseCsv(file);
        List<WeatherData> weatherDataList = new ArrayList<>();
        int successCount = 0;

        // 기상청 데이터 시간 포맷 (예: 2024-03-01 14:00)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // 3. 파싱된 데이터를 WeatherData 엔티티로 변환 (첫 줄은 헤더이므로 index 1부터 시작)
        for (int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            try {
                // ⚠️ 주의: CSV 파일의 실제 열(Column) 순서에 맞게 인덱스 번호를 조절해야 해!
                // 기본 기상청 ASOS 양식 가정 (0:지점, 1:지점명, 2:일시, 3:기온, 4:강수량, 5:풍속... 등)
                double temperature = row[3].trim().isEmpty() ? 0.0 : Double.parseDouble(row[3].trim());
                double humidity = row[5].trim().isEmpty() ? 0.0 : Double.parseDouble(row[5].trim());

                // 일사량 등은 데이터에 없는 경우가 많으므로 배열 길이 체크
                double irradiance = (row.length > 6 && !row[6].trim().isEmpty()) ? Double.parseDouble(row[6].trim()) : 0.0;
                double cloudCover = 0.0; // 운량 정보가 없다면 0.0 처리

                WeatherData data = WeatherData.builder()
                        .powerPlant(powerPlant) // 방금 찾은 발전소 매핑
                        .timestamp(LocalDateTime.parse(row[2].trim(), formatter))
                        .temperature(temperature)
                        .humidity(humidity)
                        .irradiance(irradiance)
                        .cloudCover(cloudCover)
                        .build();

                weatherDataList.add(data);
                successCount++;

            } catch (DateTimeParseException | NumberFormatException e) {
                log.warn("{}번째 줄 변환 실패 - 데이터 포맷 오류 (건너뜀): {}", i + 1, e.getMessage());
            } catch (Exception e) {
                log.warn("{}번째 줄 변환 중 알 수 없는 에러 (건너뜀): {}", i + 1, e.getMessage());
            }
        }

        // 4. DB에 일괄 저장
        weatherDataRepository.saveAll(weatherDataList);
        log.info("✅ 기상 데이터 CSV 적재 완료! 발전소 ID: {}, 총 {}건 저장", powerPlantId, successCount);

        return Map.of(
                "totalRows", csvData.size() - 1,
                "successCount", successCount
        );
    }

    /**
     * AI 서버로 예측 요청 (비동기)
     * TODO: 실제 AI 서버 URL 및 요청/응답 포맷 정의 필요
     */
    public void requestPredictionFromAi(Long powerPlantId) {
        PowerPlant plant = powerPlantRepository.findById(powerPlantId)
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다."));

        log.info("AI 서버 예측 요청 준비 완료: powerPlantId={}", powerPlantId);
        // TODO: RestTemplate을 통한 AI 서버 호출
        
    }

    /**
     * AI 예측 결과 처리 및 DB 업데이트
     */
    public void processPredictionResult(Long energyLogId, Double predictedValue) {
        EnergyLog energyLog = energyLogRepository.findById(energyLogId)
                .orElseThrow(() -> new IllegalArgumentException("에너지 로그를 찾을 수 없습니다."));

        energyLog.setPredictedGeneration(predictedValue);
        energyLogRepository.save(energyLog);

        log.info("AI 예측 결과 저장 완료: energyLogId={}, 예측값={}", energyLogId, predictedValue);
    }
}


