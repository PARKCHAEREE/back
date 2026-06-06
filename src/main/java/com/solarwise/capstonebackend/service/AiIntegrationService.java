package com.solarwise.capstonebackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solarwise.capstonebackend.dto.ai.AiPredictionRequest;
import com.solarwise.capstonebackend.dto.ai.HistoryDataDto;
import com.solarwise.capstonebackend.dto.ai.WeatherForecastDto;
import com.solarwise.capstonebackend.entity.PlantFeatureLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.exception.BusinessException;
import com.solarwise.capstonebackend.repository.PlantFeatureLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiIntegrationService {

    private final WebClient webClient;
    private final PowerPlantRepository powerPlantRepository;
    private final PlantFeatureLogRepository plantFeatureLogRepository;
    private final SimulationService simulationService;
    private final ObjectMapper objectMapper;

    @Value("${ai.server.base-url}")
    private String aiServerBaseUrl;

    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);

    @Async
    public CompletableFuture<Map> requestDashboard(Long plantId, String imageFileName) {
        return callAiServerMultipart("dashboard", plantId, imageFileName, Map.class, true);
    }

    @Async
    public CompletableFuture<Map> requestAnomalyXai(Long plantId, String imageFileName) {
        return callAiServerMultipart("anomaly/xai", plantId, imageFileName, Map.class, true);
    }

    @Async
    public CompletableFuture<Map> requestAnomalyDetail(Long plantId, String imageFileName) {
        return callAiServerMultipart("anomaly/detail", plantId, imageFileName, Map.class, true);
    }

    @Async
    public CompletableFuture<Map> requestPowerForecast(Long plantId, LocalDateTime targetTime) {
        return callAiServer("power/forecast", buildAiPredictionRequest(plantId, targetTime), Map.class);
    }

    @Async
    public CompletableFuture<Map> requestChatTurn(Map<String, Object> chatRequest) {
        return callAiServer("chat/turn", chatRequest, Map.class);
    }

    private <T> CompletableFuture<T> callAiServerMultipart(String endpoint, Long plantId, String imageFileName, Class<T> responseType, boolean geminiEnabled) {
        powerPlantRepository.findById(plantId)
                .orElseThrow(() -> new BusinessException("발전소를 찾을 수 없습니다. ID: " + plantId, HttpStatus.NOT_FOUND));
        try {
            String powerDataJson = buildPowerDataJson(plantId);
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("power_data", powerDataJson);
            bodyBuilder.part("gemini_enabled", String.valueOf(geminiEnabled));
            if (imageFileName != null && !imageFileName.isEmpty()) {
                // 💡 경로 문제 해결: 'images/' 접두사를 붙여 ClassPathResource를 생성
                bodyBuilder.part("panel_image", new ClassPathResource("images/" + imageFileName));
            }
            return webClient.post()
                    .uri(aiServerBaseUrl + "/xai/" + endpoint)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(responseType)
                    .retryWhen(getRetrySpec())
                    .doOnError(e -> log.error("AI 서버 '{}' 통신 최종 실패: {}", endpoint, e.getMessage()))
                    .toFuture();
        } catch (Exception e) {
            log.error("AI 서버 '{}' 요청 준비 중 오류 발생: {}", endpoint, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private <T> CompletableFuture<T> callAiServer(String endpoint, Object requestBody, Class<T> responseType) {
        return webClient.post()
                .uri(aiServerBaseUrl + "/xai/" + endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(responseType)
                .retryWhen(getRetrySpec())
                .doOnError(e -> log.error("AI 서버 '{}' 통신 최종 실패: {}", endpoint, e.getMessage()))
                .toFuture();
    }

    private Retry getRetrySpec() {
        return Retry.from(companion -> companion.flatMap(signal -> {
            Throwable throwable = signal.failure();
            if (signal.totalRetries() >= MAX_RETRIES) {
                return Mono.error(new BusinessException("최대 재시도 횟수(" + MAX_RETRIES + "회)를 초과했습니다.", HttpStatus.INTERNAL_SERVER_ERROR));
            }
            if (throwable instanceof WebClientResponseException ex) {
                HttpStatusCode status = ex.getStatusCode();
                if (status.value() == 429) {
                    Duration delay = extractRetryDelay(ex);
                    log.warn("429 Too Many Requests. {}초 후 재시도합니다... (시도 {}/{})", delay.toSeconds(), signal.totalRetries() + 1, MAX_RETRIES);
                    return Mono.delay(delay);
                }
                if (status.is5xxServerError()) {
                    Duration delay = Duration.ofSeconds((long) Math.pow(2, signal.totalRetries()));
                    log.warn("서버 오류 ({}). {}초 후 재시도합니다... (시도 {}/{})", status, delay.toSeconds(), signal.totalRetries() + 1, MAX_RETRIES);
                    return Mono.delay(delay);
                }
            }
            return Mono.error(throwable);
        }));
    }

    private Duration extractRetryDelay(WebClientResponseException ex) {
        List<String> retryAfterHeader = ex.getHeaders().get("Retry-After");
        if (retryAfterHeader != null && !retryAfterHeader.isEmpty()) {
            try {
                return Duration.ofSeconds(Long.parseLong(retryAfterHeader.get(0)));
            } catch (NumberFormatException e) { log.warn("잘못된 Retry-After 헤더 값: {}", retryAfterHeader.get(0)); }
        }
        String responseBody = ex.getResponseBodyAsString();
        if (responseBody != null) {
            Pattern pattern = Pattern.compile("Please retry in (\\d+\\.?\\d*)s");
            Matcher matcher = pattern.matcher(responseBody);
            if (matcher.find()) {
                try {
                    return Duration.ofMillis((long) (Double.parseDouble(matcher.group(1)) * 1000));
                } catch (NumberFormatException e) { log.warn("응답 본문에서 지연 시간 파싱 실패: {}", matcher.group(1)); }
            }
        }
        return INITIAL_BACKOFF;
    }

    private String buildPowerDataJson(Long plantId) throws JsonProcessingException {
        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        PlantFeatureLog data = plantFeatureLogRepository.findByPowerPlantIdAndMeasuredAt(plantId, virtualNow)
                .orElseGet(() -> plantFeatureLogRepository.findTopByPowerPlantIdOrderByMeasuredAtDesc(plantId));
        if (data == null) {
            log.warn("발전소 {}에 대한 기상 데이터를 찾을 수 없습니다. 더미 데이터로 진행", plantId);
            data = createDummyPlantFeatureLog(plantId, virtualNow);
        }
        Map<String, Object> powerData = new HashMap<>();
        powerData.put("measured_at", data.getMeasuredAt().format(DateTimeFormatter.ISO_DATE_TIME));
        powerData.put("temp", nullSafeDouble(data.getTemp()));
        powerData.put("humi", nullSafeDouble(data.getHumi()));
        powerData.put("clou", nullSafeDouble(data.getClou()));
        powerData.put("wisp", nullSafeDouble(data.getWisp()));
        powerData.put("hSin", nullSafeDouble(data.getHSin()));
        powerData.put("hCos", nullSafeDouble(data.getHCos()));
        powerData.put("doySin", nullSafeDouble(data.getDoySin()));
        powerData.put("doyCos", nullSafeDouble(data.getDoyCos()));
        powerData.put("wideSin", nullSafeDouble(data.getWideSin()));
        powerData.put("wideCos", nullSafeDouble(data.getWideCos()));
        powerData.put("sunElevClip", nullSafeDouble(data.getSunElevClip()));
        powerData.put("cosZen", nullSafeDouble(data.getCosZen()));
        powerData.put("irradiance", nullSafeDouble(data.getIrradiance()));
        powerData.put("estIrradiance", nullSafeDouble(data.getEstIrradiance()));
        powerData.put("irradianceProxy", nullSafeDouble(data.getIrradianceProxy()));
        powerData.put("irradianceXCapa", nullSafeDouble(data.getIrradianceXCapa()));
        powerData.put("capa", nullSafeDouble(data.getCapa()));
        powerData.put("seasonalSolarPattern", nullSafeDouble(data.getSeasonalSolarPattern()));
        powerData.put("weatherAdjustedPattern", nullSafeDouble(data.getWeatherAdjustedPattern()));
        powerData.put("expectedGenProxy", nullSafeDouble(data.getExpectedGenProxy()));
        powerData.put("genLag2", nullSafeDouble(data.getGenLag2()));
        powerData.put("dustCoverageRatio", nullSafeDouble(data.getDustCoverageRatio()));
        powerData.put("snowCoverageRatio", nullSafeDouble(data.getSnowCoverageRatio()));
        powerData.put("birdDroppingCount", nullSafeInteger(data.getBirdDroppingCount()));
        powerData.put("physicalDamageCount", nullSafeInteger(data.getPhysicalDamageCount()));
        powerData.put("maxDefectConfidence", nullSafeDouble(data.getMaxDefectConfidence()));
        powerData.put("clsNormal", nullSafeInteger(data.getClsNormal()));
        powerData.put("clsDust", nullSafeInteger(data.getClsDust()));
        powerData.put("clsSnow", nullSafeInteger(data.getClsSnow()));
        powerData.put("clsBird", nullSafeInteger(data.getClsBird()));
        powerData.put("clsDamage", nullSafeInteger(data.getClsDamage()));
        powerData.put("prediction", nullSafeDouble(data.getPrediction()));
        powerData.put("actual", nullSafeDouble(data.getActual()));
        return objectMapper.writeValueAsString(powerData);
    }

    private AiPredictionRequest buildAiPredictionRequest(Long plantId, LocalDateTime targetTime) {
        PowerPlant plant = powerPlantRepository.findById(plantId)
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다. ID: " + plantId));
        List<HistoryDataDto> history = new ArrayList<>();
        history.add(HistoryDataDto.builder().timestamp(targetTime.minusHours(1)).actualPowerKw(50.0).build());
        List<WeatherForecastDto> weatherForecast = new ArrayList<>();
        weatherForecast.add(WeatherForecastDto.builder().forecastTime(targetTime).irradiation(800.0).ambientTemperature(25.0).build());
        return AiPredictionRequest.builder()
                .plantId("PLANT_" + String.format("%03d", plant.getId()))
                .requestedAt(targetTime)
                .history(history)
                .weatherForecast(weatherForecast)
                .build();
    }

    private PlantFeatureLog createDummyPlantFeatureLog(Long plantId, LocalDateTime time) {
        return PlantFeatureLog.builder()
                .powerPlantId(plantId)
                .measuredAt(time)
                .temp(25.0).humi(60.0).clou(0.5).wisp(2.0).irradiance(800.0)
                .prediction(100.0).actual(95.0)
                .build();
    }

    private double nullSafeDouble(Double value) { return value != null ? value : 0.0; }
    private int nullSafeInteger(Integer value) { return value != null ? value : 0; }
}
