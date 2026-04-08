package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.ai.AiApiResponse;
import com.solarwise.capstonebackend.dto.ai.AiPredictionRequest;
import com.solarwise.capstonebackend.dto.ai.AiPredictionResponse;
import com.solarwise.capstonebackend.dto.ai.XaiExplanationResponse;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
/*
 * 외부 AI 서버 및 기상청 API와의 통신을 담당하는 서비스 클래스입니다.
 * <p>
 * 발전량 예측, 예측 설명(XAI), 실시간 날씨 데이터 조회, 과거 데이터 업로드 등의 기능을 제공합니다.
 * </p>
 */
public class AiIntegrationService {

    private final RestTemplate restTemplate;
    private final PowerPlantRepository powerPlantRepository;
    private final EnergyLogRepository energyLogRepository;
    private final WeatherDataFormatterUtil weatherDataFormatterUtil;

    private final CsvParsingUtil csvParsingUtil;
    private final WeatherDataRepository weatherDataRepository;

    @Value("${kma.api.key}")
    private String apiKey;

    @Value("${ai.server.base-url}")
    private String aiServerBaseUrl;

    /**
     * 기상청 단기예보 API를 호출하여 특정 위치의 실시간 날씨 데이터를 조회합니다.
     *
     * @param nx 예보지점 X 좌표
     * @param ny 예보지점 Y 좌표
     * @return 날씨 데이터 (기온, 습도, 풍속 등)를 담은 Map
     * @throws RuntimeException 기상청 API 통신 중 오류 발생 시
     */
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

    /**
     * 과거 기상 데이터가 담긴 CSV 파일을 업로드하여 DB에 저장합니다.
     *
     * @param powerPlantId 기상 데이터를 매핑할 발전소의 ID
     * @param file         업로드된 CSV 파일 (multipart/form-data)
     * @return 성공 및 실패 건수를 포함한 처리 결과 Map
     * @throws IllegalArgumentException 존재하지 않는 발전소 ID일 경우
     */
    @Transactional
    public Map<String, Object> uploadWeatherDataCsv(Long powerPlantId, MultipartFile file) {
        PowerPlant powerPlant = powerPlantRepository.findById(powerPlantId)
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + powerPlantId));

        List<String[]> csvData = csvParsingUtil.parseCsv(file);
        List<WeatherData> weatherDataList = new ArrayList<>();
        int successCount = 0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // CSV 데이터를 WeatherData 엔티티로 변환 (첫 줄은 헤더이므로 index 1부터 시작)
        for (int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            try {
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

        weatherDataRepository.saveAll(weatherDataList);
        log.info("기상 데이터 CSV 적재 완료! 발전소 ID: {}, 총 {}건 저장", powerPlantId, successCount);

        return Map.of(
                "totalRows", csvData.size() - 1,
                "successCount", successCount
        );
    }

    /**
     * AI 서버로 발전량 예측을 요청합니다.
     *
     * @param powerPlantId 예측을 원하는 발전소의 ID
     * @return AI 서버로부터 받은 예측 결과 데이터
     * @throws IllegalArgumentException 존재하지 않는 발전소 ID일 경우
     * @throws RuntimeException         AI 서버 통신 중 오류 발생 시
     */
    public AiPredictionResponse requestPredictionFromAi(Long powerPlantId) {
        log.info("AI 서버 예측 요청 시작: 발전소 ID={}", powerPlantId);

        AiPredictionRequest requestDto = buildAiRequest(powerPlantId);

        String url = aiServerBaseUrl + "/predict/generation";
        log.debug("AI 서버 요청 URL: {}", url);
        log.debug("AI 서버 요청 Body: {}", requestDto);

        try {
            ParameterizedTypeReference<AiApiResponse<AiPredictionResponse>> responseType = new ParameterizedTypeReference<>() {};

            ResponseEntity<AiApiResponse<AiPredictionResponse>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(requestDto),
                    responseType
            );

            AiApiResponse<AiPredictionResponse> apiResponse = responseEntity.getBody();

            if (apiResponse == null || !"success".equals(apiResponse.getStatus()) || apiResponse.getData() == null) {
                log.error("AI 서버 응답 오류: {}", apiResponse);
                throw new RuntimeException("AI 서버로부터 유효한 응답을 받지 못했습니다.");
            }

            log.info("AI 서버 예측 결과 수신 성공: {}", apiResponse.getData());
            return apiResponse.getData();

        } catch (RestClientException e) {
            log.error("AI 서버 통신 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서버 통신 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * AI 서버로 예측 결과에 대한 설명을 요청합니다. (XAI)
     *
     * @param powerPlantId 예측 설명을 원하는 발전소의 ID
     * @return AI 서버로부터 받은 예측 설명 데이터
     * @throws IllegalArgumentException 존재하지 않는 발전소 ID일 경우
     * @throws RuntimeException         AI 서버 통신 중 오류 발생 시
     */
    public XaiExplanationResponse requestXaiExplanation(Long powerPlantId) {
        log.info("AI 서버 XAI 요청 시작: 발전소 ID={}", powerPlantId);

        AiPredictionRequest requestDto = buildAiRequest(powerPlantId);

        String url = aiServerBaseUrl + "/explain/generation"; // XAI 엔드포인트
        log.debug("AI 서버 XAI 요청 URL: {}", url);
        log.debug("AI 서버 XAI 요청 Body: {}", requestDto);

        try {
            ParameterizedTypeReference<AiApiResponse<XaiExplanationResponse>> responseType = new ParameterizedTypeReference<>() {};

            ResponseEntity<AiApiResponse<XaiExplanationResponse>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(requestDto),
                    responseType
            );

            AiApiResponse<XaiExplanationResponse> apiResponse = responseEntity.getBody();

            if (apiResponse == null || !"success".equals(apiResponse.getStatus()) || apiResponse.getData() == null) {
                log.error("AI 서버 XAI 응답 오류: {}", apiResponse);
                throw new RuntimeException("AI 서버로부터 유효한 XAI 응답을 받지 못했습니다.");
            }

            log.info("AI 서버 XAI 결과 수신 성공: {}", apiResponse.getData());
            return apiResponse.getData();

        } catch (RestClientException e) {
            log.error("AI 서버 XAI 통신 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서버 XAI 통신 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * AI 서버에 보낼 요청 DTO(Data Transfer Object)를 생성합니다.
     * 실시간 날씨 데이터를 조회하여 요청에 필요한 파라미터를 구성합니다.
     *
     * @param powerPlantId 요청을 보낼 발전소의 ID
     * @return 생성된 {@link AiPredictionRequest} 객체
     * @throws IllegalArgumentException 존재하지 않는 발전소 ID일 경우
     */
    private AiPredictionRequest buildAiRequest(Long powerPlantId) {
        PowerPlant plant = powerPlantRepository.findById(powerPlantId)
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + powerPlantId));

        // TODO: 발전소 Entity에 기상청 API 호출을 위한 nx, ny 좌표가 저장되어 있어야 합니다. 여기서는 임시값을 사용합니다.
        int nx = 60; // 예시: 서울특별시
        int ny = 127;
        Map<String, Double> weatherData = fetchRealTimeWeather(nx, ny);

        LocalDateTime now = LocalDateTime.now();
        Double ambientTemperature = weatherData.getOrDefault("T1H", 0.0);
        Double irradiation = weatherData.getOrDefault("IRR", 0.0);
        Double moduleTemperature = ambientTemperature + (irradiation * 20); // 간단한 추정식

        return new AiPredictionRequest(
                "PLANT_" + String.format("%03d", plant.getId()),
                now, irradiation, ambientTemperature, moduleTemperature,
                weatherData.getOrDefault("WSD", 0.0), weatherData.getOrDefault("REH", 0.0));
    }

    /**
     * AI 예측 결과 처리 및 DB 업데이트
     *
     * @param energyLogId     예측값을 저장할 에너지 로그의 ID
     * @param predictedValue  AI 서버로부터 받은 예측 발전량
     * @throws IllegalArgumentException 존재하지 않는 에너지 로그 ID일 경우
     */
    public void processPredictionResult(Long energyLogId, Double predictedValue) {
        EnergyLog energyLog = energyLogRepository.findById(energyLogId)
                .orElseThrow(() -> new IllegalArgumentException("에너지 로그를 찾을 수 없습니다."));

        energyLog.setPredictedGeneration(predictedValue);
        energyLogRepository.save(energyLog);

        log.info("AI 예측 결과 저장 완료: energyLogId={}, 예측값={}", energyLogId, predictedValue);
    }
}
