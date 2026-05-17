package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.ai.AiApiResponse;
import com.solarwise.capstonebackend.dto.ai.AiPredictionRequest;
import com.solarwise.capstonebackend.dto.ai.AiPredictionResponse;
import com.solarwise.capstonebackend.dto.ai.HistoryDataDto;
import com.solarwise.capstonebackend.dto.ai.PowerAnomalyDetectionRequest;
import com.solarwise.capstonebackend.dto.ai.PowerAnomalyDetectionResponse;
import com.solarwise.capstonebackend.dto.ai.VisionAnomalyDetectionResponse;
import com.solarwise.capstonebackend.dto.ai.WeatherForecastDto;
import com.solarwise.capstonebackend.dto.ai.XaiExplanationRequest;
import com.solarwise.capstonebackend.dto.ai.XaiExplanationResponse;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.entity.Forecast;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.entity.VisionAnalysis;
import com.solarwise.capstonebackend.exception.BusinessException;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.ForecastRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.repository.VisionAnalysisRepository;
import com.solarwise.capstonebackend.util.CsvParsingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    private final PlantFeatureLogRepository plantFeatureLogRepository;
    private final ForecastRepository forecastRepository;
    private final AnomalyRepository anomalyRepository;

    private final CsvParsingUtil csvParsingUtil;
    private final VisionAnalysisRepository visionAnalysisRepository;
    private final SimulationService simulationService;
    private final NotificationService notificationService;

    @Value("${ai.server.base-url}")
    private String aiServerBaseUrl;

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
        List<PlantFeatureLog> weatherDataList = new ArrayList<>();
        int successCount = 0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // CSV 데이터를 WeatherData 엔티티로 변환 (첫 줄은 헤더이므로 index 1부터 시작)
        for (int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            try {
                double temperature = row[3].trim().isEmpty() ? 0.0 : Double.parseDouble(row[3].trim());
                double humidity = row[5].trim().isEmpty() ? 0.0 : Double.parseDouble(row[5].trim());

                // 일사량 등은 데이터에 없는 경우가 많으므로 배열 길이 체크
                double irradiance = 0.0; // 향후 OpenWeather API 연동을 위해 빈칸으로 두고 고정값 설정
                double cloudCover = 0.0; // 운량 정보가 없다면 0.0 처리

                PlantFeatureLog data = PlantFeatureLog.builder()
                        .powerPlantId(powerPlant.getId())
                        .measuredAt(LocalDateTime.parse(row[2].trim(), formatter))
                        .temp(temperature)
                        .humi(humidity)
                        .irradiance(irradiance)
                        .clou(cloudCover)
                        .actual(0.0) // 기상 데이터에는 발전량 없음
                        .prediction(0.0)
                        .wisp(0.0)
                        .build();

                weatherDataList.add(data);
                successCount++;

            } catch (DateTimeParseException | NumberFormatException e) {
                log.warn("{}번째 줄 변환 실패 - 데이터 포맷 오류 (건너뜀): {}", i + 1, e.getMessage());
            } catch (Exception e) {
                log.warn("{}번째 줄 변환 중 알 수 없는 에러 (건너뜀): {}", i + 1, e.getMessage());
            }
        }

        plantFeatureLogRepository.saveAll(weatherDataList);
        log.info("기상 데이터 CSV 적재 완료! 발전소 ID: {}, 총 {}건 저장", powerPlantId, successCount);

        return Map.of(
                "totalRows", csvData.size() - 1,
                "successCount", successCount
        );
    }

    /**
     * AI 서버로 발전량 예측을 요청합니다.
     * 응답을 받으면 Forecast 엔티티로 변환하여 DB에 저장합니다.
     *
     * @param powerPlantId 예측을 원하는 발전소의 ID
     * @return AI 서버로부터 받은 예측 결과 데이터
     * @throws IllegalArgumentException 존재하지 않는 발전소 ID일 경우
     * @throws RuntimeException         AI 서버 통신 중 오류 발생 시
     */
    @Async
    public CompletableFuture<AiPredictionResponse> requestPredictionFromAi(Long powerPlantId) {
        log.info("AI 서버 예측 요청 시작: 발전소 ID={}", powerPlantId);

        PowerPlant powerPlant = powerPlantRepository.findById(powerPlantId)
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + powerPlantId));

        AiPredictionRequest requestDto = buildAiRequest(powerPlantId);

        String url = aiServerBaseUrl + "/internal/forecast/predict";
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

            AiPredictionResponse predictionResponse = apiResponse.getData();
            log.info("AI 서버 예측 결과 수신 성공: {}", predictionResponse);

            // 예측 결과를 Forecast 엔티티로 변환하여 DB에 저장
            saveForecastsToDB(powerPlant, predictionResponse);

            return CompletableFuture.completedFuture(predictionResponse);

        } catch (RestClientException e) {
            log.error("AI 서버 통신 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서버 통신 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * AI 예측 응답을 Forecast 엔티티로 변환하여 DB에 저장합니다.
     *
     * @param powerPlant 발전소 엔티티
     * @param response AI 서버의 예측 응답
     */
    @Transactional
    protected void saveForecastsToDB(PowerPlant powerPlant, AiPredictionResponse response) {
        if (response.getForecastSeries() == null || response.getForecastSeries().isEmpty()) {
            log.warn("예측 시계열 데이터가 없습니다.");
            return;
        }

        List<Forecast> forecasts = new ArrayList<>();
        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        for (com.solarwise.capstonebackend.dto.ai.ForecastDto forecastDto : response.getForecastSeries()) {
            Forecast forecast = Forecast.builder()
                    .powerPlant(powerPlant)
                    .targetTime(forecastDto.getTargetTime())
                    .predictedPowerKw(forecastDto.getPredictedPowerKw())
                    .confidence(forecastDto.getConfidence())
                    .modelVersion(forecastDto.getModelVersion())
                    .modelNotes(forecastDto.getModelNotes())
                    .status("COMPLETED")
                    .createdAt(virtualNow)
                    .updatedAt(virtualNow)
                    .build();
            forecasts.add(forecast);
        }

        forecastRepository.saveAll(forecasts);
        log.info("발전량 예측 결과 {}건이 DB에 저장되었습니다. 발전소 ID: {}", forecasts.size(), powerPlant.getId());
    }

    /**
     * AI 서버로 예측 결과에 대한 설명을 요청합니다. (XAI)
     *
     * @param powerPlantId 예측 설명을 원하는 발전소의 ID
     * @return AI 서버로부터 받은 예측 설명 데이터
     * @throws IllegalArgumentException 존재하지 않는 발전소 ID일 경우
     * @throws RuntimeException         AI 서버 통신 중 오류 발생 시
     */
    @Async
    public CompletableFuture<XaiExplanationResponse> requestXaiExplanation(Long powerPlantId) {
        log.info("AI 서버 XAI 요청 시작: 발전소 ID={}", powerPlantId);

        PowerPlant powerPlant = powerPlantRepository.findById(powerPlantId)
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + powerPlantId));

        // 최근 이상 탐지 이벤트 ID 조회 (없으면 임시값 사용)
        String eventId = "EVENT_" + powerPlantId + "_" + System.currentTimeMillis();

        // 최근 예측 데이터 조회 (없으면 임시값 사용)
        Double forecast = 100.0; // TODO: ForecastRepository에서 최근 예측값 조회
        Double actual = 95.0;   // TODO: EnergyLogRepository에서 최근 실제값 조회

        // 기상 데이터 조회 (시뮬레이션 모드: 고정값 사용)
        LocalDateTime now = simulationService.getVirtualCurrentTime();
        Double ambientTemperature = 25.0; // 시뮬레이션 고정값
        Double irradiation = 800.0; // 시뮬레이션 고정값
        Double windSpeed = 2.0; // 시뮬레이션 고정값
        Double humidity = 60.0; // 시뮬레이션 고정값
        Double moduleTemperature = ambientTemperature + (irradiation * 20);

        // 명세서 6-4에 맞는 XAI 요청 DTO 생성
        XaiExplanationRequest.XaiContext.WeatherInfo weatherInfo = XaiExplanationRequest.XaiContext.WeatherInfo.builder()
                .irradiation(irradiation)
                .ambientTemperature(ambientTemperature)
                .moduleTemperature(moduleTemperature)
                .windSpeed(windSpeed)
                .humidity(humidity)
                .build();

        XaiExplanationRequest.XaiContext context = XaiExplanationRequest.XaiContext.builder()
                .anomalyType("POWER") // 전력 기반 이상 탐지
                .forecast(forecast)
                .actual(actual)
                .weather(weatherInfo)
                .build();

        XaiExplanationRequest requestDto = XaiExplanationRequest.builder()
                .plantId("PLANT_" + String.format("%03d", powerPlant.getId()))
                .eventId(eventId)
                .context(context)
                .build();

        String url = aiServerBaseUrl + "/internal/xai/explain"; // XAI 엔드포인트
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

            XaiExplanationResponse result = apiResponse.getData();
            log.info("AI 서버 XAI 결과 수신 성공: {}", result);

            return CompletableFuture.completedFuture(result);

        } catch (RestClientException e) {
            log.error("AI 서버 XAI 통신 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서버 XAI 통신 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * AI 서버에 보낼 요청 DTO(Data Transfer Object)를 생성합니다.
     * 히스토리 데이터 및 실시간 날씨 데이터를 조회하여 요청에 필요한 파라미터를 구성합니다.
     *
     * @param powerPlantId 요청을 보낼 발전소의 ID
     * @return 생성된 {@link AiPredictionRequest} 객체
     * @throws IllegalArgumentException 존재하지 않는 발전소 ID일 경우
     */
    private AiPredictionRequest buildAiRequest(Long powerPlantId) {
        PowerPlant plant = powerPlantRepository.findById(powerPlantId)
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + powerPlantId));

        // TODO: 발전소 Entity에 기상청 API 호출을 위한 nx, ny 좌표가 저장되어 있어야 합니다. 여기서는 임시값을 사용합니다.

        // 시뮬레이션 모드: 고정 기상 데이터 사용
        LocalDateTime now = simulationService.getVirtualCurrentTime();
        Double ambientTemperature = 25.0; // 시뮬레이션 고정값
        Double irradiation = 800.0; // 시뮬레이션 고정값
        Double windSpeed = 2.0; // 시뮬레이션 고정값
        Double humidity = 60.0; // 시뮬레이션 고정값
        Double moduleTemperature = ambientTemperature + (irradiation * 20); // 간단한 추정식

        // 과거 데이터 (히스토리) 조회
        List<HistoryDataDto> history = buildHistoryData(plant);

        // 날씨 예보 (단일 시점 또는 시계열)
        List<WeatherForecastDto> weatherForecast = buildWeatherForecast(
                now, ambientTemperature, moduleTemperature, irradiation, windSpeed, humidity);

        return AiPredictionRequest.builder()
                .plantId("PLANT_" + String.format("%03d", plant.getId()))
                .requestedAt(now)
                .history(history)
                .weatherForecast(weatherForecast)
                .build();
    }

    /**
     * 발전소의 과거 에너지 데이터 및 날씨 데이터를 조회하여 히스토리 DTO 리스트를 생성합니다.
     *
     * @param plant 대상 발전소
     * @return 히스토리 데이터 DTO 리스트
     */
    private List<HistoryDataDto> buildHistoryData(PowerPlant plant) {
        List<HistoryDataDto> history = new ArrayList<>();

        // TODO: EnergyLogRepository에서 과거 데이터를 조회하고 HistoryDataDto로 변환
        // 예시 구현 (실제로는 DB에서 조회):
        // List<EnergyLog> energyLogs = energyLogRepository.findByPowerPlantAndTimestampAfter(plant, pastDate);
        // history = energyLogs.stream()
        //     .map(log -> HistoryDataDto.builder()
        //         .timestamp(log.getTimestamp())
        //         .actualPowerKw(log.getActualGeneration())
        //         .irradiation(0.5) // TODO: WeatherData에서 조회
        //         .build())
        //     .collect(Collectors.toList());

        log.debug("히스토리 데이터 {}건 구성", history.size());
        return history;
    }

    /**
     * 현재 기상 데이터를 기반으로 날씨 예보 DTO 리스트를 생성합니다.
     *
     * @param forecastTime 예보 시간
     * @param ambientTemp 주변 온도
     * @param moduleTemp 모듈 온도
     * @param irradiation 일사량
     * @param windSpeed 풍속
     * @param humidity 습도
     * @return 날씨 예보 DTO 리스트
     */
    private List<WeatherForecastDto> buildWeatherForecast(
            LocalDateTime forecastTime, Double ambientTemp, Double moduleTemp,
            Double irradiation, Double windSpeed, Double humidity) {

        List<WeatherForecastDto> forecasts = new ArrayList<>();

        // 현재 시점의 날씨를 단일 예보로 추가
        WeatherForecastDto currentForecast = WeatherForecastDto.builder()
                .forecastTime(forecastTime)
                .ambientTemperature(ambientTemp)
                .moduleTemperature(moduleTemp)
                .irradiation(irradiation)
                .windSpeed(windSpeed)
                .humidity(humidity)
                .cloudCover(0.5) // TODO: 실제 구름 덮음 정보 조회
                .build();

        forecasts.add(currentForecast);

        // TODO: 추가 시점의 예보 데이터 생성 (시계열 예측 필요시)

        log.debug("날씨 예보 {}건 구성", forecasts.size());
        return forecasts;
    }

    /**
     * 전력 데이터 기반의 이상 탐지를 AI 서버에 요청합니다.
     * 발전량 저하, 패널 결함 등을 탐지합니다.
     *
     * @param powerPlantId 이상 탐지를 원하는 발전소의 ID
     * @param actualPower 실제 발전량 (kW)
     * @param predictedPower 예측 발전량 (kW)
     * @return AI 서버로부터 받은 이상 탐지 결과
     * @throws IllegalArgumentException 존재하지 않는 발전소 ID일 경우
     * @throws RuntimeException AI 서버 통신 중 오류 발생 시
     */
    @Async
    public CompletableFuture<PowerAnomalyDetectionResponse> detectPowerAnomaly(Long powerPlantId, Double actualPower, Double predictedPower) {
        log.info("AI 서버 전력 이상 탐지 요청 시작: 발전소 ID={}", powerPlantId);

        PowerPlant powerPlant = powerPlantRepository.findById(powerPlantId)
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + powerPlantId));

        // 기상 데이터 조회 (시뮬레이션 모드: 고정값 사용)
        LocalDateTime now = simulationService.getVirtualCurrentTime();
        Double ambientTemperature = 25.0; // 시뮬레이션 고정값
        Double irradiance = 800.0; // 시뮬레이션 고정값
        Double moduleTemperature = ambientTemperature + (irradiance * 20);

        PowerAnomalyDetectionRequest requestDto = PowerAnomalyDetectionRequest.builder()
                .plantId("PLANT_" + String.format("%03d", powerPlant.getId()))
                .panelId(null) // 발전소 전체 이상 탐지
                .datetime(now)
                .actualPower(actualPower)
                .predictedPower(predictedPower)
                .irradiation(irradiance)
                .ambientTemperature(ambientTemperature)
                .moduleTemperature(moduleTemperature)
                .build();

        String url = aiServerBaseUrl + "/internal/anomaly/power-detect";
        log.debug("AI 서버 전력 이상 탐지 요청 URL: {}", url);

        try {
            ParameterizedTypeReference<AiApiResponse<PowerAnomalyDetectionResponse>> responseType =
                    new ParameterizedTypeReference<>() {};

            ResponseEntity<AiApiResponse<PowerAnomalyDetectionResponse>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(requestDto),
                    responseType
            );

            AiApiResponse<PowerAnomalyDetectionResponse> apiResponse = responseEntity.getBody();

            if (apiResponse == null || !"success".equals(apiResponse.getStatus()) || apiResponse.getData() == null) {
                log.error("AI 서버 전력 이상 탐지 응답 오류: {}", apiResponse);
                throw new RuntimeException("AI 서버로부터 유효한 응답을 받지 못했습니다.");
            }

            PowerAnomalyDetectionResponse result = apiResponse.getData();
            log.info("AI 서버 전력 이상 탐지 결과 수신: 이상여부={}, 점수={}", result.getIsAnomaly(), result.getAnomalyScore());

            // 이상 탐지 결과를 Anomaly 엔티티로 저장
            if (result.getIsAnomaly()) {
                saveAnomalyToDB(powerPlant, result);
            }

            return CompletableFuture.completedFuture(result);

        } catch (RestClientException e) {
            log.error("AI 서버 전력 이상 탐지 통신 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서버 전력 이상 탐지 통신 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 패널 이미지 분석을 통한 이상 탐지를 AI 서버에 요청합니다.
     * 먼지, 눈, 파손 등을 탐지합니다.
     *
     * @param powerPlantId 이미지 분석을 원하는 발전소의 ID
     * @param panelId 패널 ID
     * @param imageBase64 이미지 Base64 인코딩 문자열
     * @return AI 서버로부터 받은 이미지 분석 결과
     * @throws IllegalArgumentException 존재하지 않는 발전소 ID일 경우
     * @throws RuntimeException AI 서버 통신 중 오류 발생 시
     */
    @Async
    public CompletableFuture<VisionAnomalyDetectionResponse> detectVisionAnomaly(Long powerPlantId, String panelId, String imageBase64) {
        log.info("AI 서버 이미지 이상 탐지 요청 시작: 발전소 ID={}, 패널 ID={}", powerPlantId, panelId);

        PowerPlant powerPlant = powerPlantRepository.findById(powerPlantId)
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + powerPlantId));

        LocalDateTime now = simulationService.getVirtualCurrentTime();

        // Note: 실제 구��에서는 multipart/form-data로 이미지 전송
        // 여기서는 BASE64 인코딩된 이미지 데이터를 가정
        Map<String, Object> requestBody = Map.of(
                "plant_id", "PLANT_" + String.format("%03d", powerPlant.getId()),
                "panel_id", panelId,
                "captured_at", now.format(DateTimeFormatter.ISO_DATE_TIME),
                "image_base64", imageBase64
        );

        String url = aiServerBaseUrl + "/internal/anomaly/vision-detect";
        log.debug("AI 서버 이미지 이상 탐지 요청 URL: {}", url);

        try {
            ParameterizedTypeReference<AiApiResponse<VisionAnomalyDetectionResponse>> responseType =
                    new ParameterizedTypeReference<>() {};

            ResponseEntity<AiApiResponse<VisionAnomalyDetectionResponse>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody),
                    responseType
            );

            AiApiResponse<VisionAnomalyDetectionResponse> apiResponse = responseEntity.getBody();

            if (apiResponse == null || !"success".equals(apiResponse.getStatus()) || apiResponse.getData() == null) {
                log.error("AI 서버 이미지 이상 탐지 응답 오류: {}", apiResponse);
                throw new RuntimeException("AI 서버로부터 유효한 응답을 받지 못했습니다.");
            }

            VisionAnomalyDetectionResponse result = apiResponse.getData();
            log.info("AI 서버 이미지 이상 탐지 결과 수신: 결함여부={}, 타입={}", result.getIsDefective(), result.getDefectType());

            // 이상 탐지 결과를 Anomaly 엔티티로 저장
            if (result.getIsDefective()) {
                saveVisionAnomalyToDB(powerPlant, panelId, result);
            }

            return CompletableFuture.completedFuture(result);

        } catch (RestClientException e) {
            log.error("AI 서버 이미지 이상 탐지 통신 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서버 이미지 이상 탐지 통신 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 전력 이상 탐지 결과를 Anomaly 엔티티로 저장합니다.
     *
     * @param powerPlant 발전소 엔티티
     * @param response AI 서버의 전력 이상 탐지 응답
     */
    @Transactional
    protected void saveAnomalyToDB(PowerPlant powerPlant, PowerAnomalyDetectionResponse response) {
        Anomaly anomaly = Anomaly.builder()
                .powerPlant(powerPlant)
                .type("POWER")
                .summary("발전량 이상 탐지")
                .description("실제 발전량과 예측 발전량 간의 편차 감지")
                .severity(response.getSeverity())
                .cause(String.format("예측값 대비 %.2f%% 편차", calculateDeviation(response)))
                .recommendedAction(response.getRecommendation())
                .status("OPEN")
                .detectedAt(simulationService.getVirtualCurrentTime())
                .build();

        Anomaly savedAnomaly = anomalyRepository.save(anomaly);
        log.info("이상 탐지 결과 DB 저장 완료: 발전소 ID={}, 심각도={}", powerPlant.getId(), response.getSeverity());

        // Lazy Loading 에러 방지: 이메일을 미리 추출한 후 전달
        String ownerEmail = powerPlant.getUser().getEmail();
        notificationService.sendAnomalyAlert(savedAnomaly, ownerEmail);
    }

    /**
     * 이미지 이상 탐지 결과를 Anomaly 엔티티로 저장합니다.
     *
     * @param powerPlant 발전소 엔티티
     * @param panelId 패널 ID
     * @param response AI 서버의 이미지 이상 탐지 응답
     */
    @Transactional
    protected void saveVisionAnomalyToDB(PowerPlant powerPlant, String panelId, VisionAnomalyDetectionResponse response) {
        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        Anomaly anomaly = Anomaly.builder()
                .powerPlant(powerPlant)
                .type("VISION")
                .summary("패널 이상 탐지")
                .description(String.format("패널 %s: %s 감지", panelId, response.getDefectType()))
                .severity(response.getSeverity())
                .cause(String.format("결함 유형: %s (신뢰도: %.2f%%)", response.getDefectType(), response.getConfidence() * 100))
                .recommendedAction(response.getRecommendation())
                .status("OPEN")
                .detectedAt(virtualNow)
                .createdAt(virtualNow)
                .updatedAt(virtualNow)
                .build();

        Anomaly savedAnomaly = anomalyRepository.save(anomaly);

        // VisionAnalysis 엔티티 저장
        VisionAnalysis visionAnalysis = VisionAnalysis.builder()
                .anomaly(savedAnomaly)
                .imageUrl(null) // TODO: 이미지 URL 저장 로직 추가
                .analysisResult(String.format("결함 유형: %s, 신뢰도: %.2f%%, 심각도: %s",
                        response.getDefectType(), response.getConfidence() * 100, response.getSeverity()))
                .createdAt(virtualNow)
                .build();

        visionAnalysisRepository.save(visionAnalysis);

        log.info("이미지 이상 탐지 결과 DB 저장 완료: 발전소 ID={}, 패널 ID={}, 심각도={}",
                powerPlant.getId(), panelId, response.getSeverity());

        // Lazy Loading 에러 방지: 이메일을 미리 추출한 후 전달
        String ownerEmail = powerPlant.getUser().getEmail();
        notificationService.sendAnomalyAlert(savedAnomaly, ownerEmail);
    }

    /**
     * 예측값과 실제값의 편차율을 계산합니다.
     */
    private Double calculateDeviation(PowerAnomalyDetectionResponse response) {
        if (response.getPredictedPower() == 0) {
            return 0.0;
        }
        return ((response.getPredictedPower() - response.getActualPower()) / response.getPredictedPower()) * 100;
    }
}
