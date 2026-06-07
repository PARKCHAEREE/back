package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.AnomalyDto;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.VisionAnalysis;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.VisionAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyService {

    private final AnomalyRepository anomalyRepository;
    private final VisionAnalysisRepository visionAnalysisRepository;
    private final AiIntegrationService aiIntegrationService;
    private final SimulationService simulationService;

    @Value("${server.address:localhost}")
    private String serverAddress;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${ai.server.base-url}")
    private String aiServerBaseUrl;

    @Transactional(readOnly = true)
    public List<AnomalyDto> getAnomalyList(Long plantId) {
        return anomalyRepository.findByPowerPlantIdOrderByDetectedAtDesc(plantId).stream()
                .map(anomaly -> toDto(anomaly, null, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnomalyDto getAnomalyDetailWithAi(Long eventId) {
        Anomaly anomaly = anomalyRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("이벤트를 찾을 수 없습니다. ID: " + eventId));

        if ("VISION".equals(anomaly.getType())) {
            Optional<VisionAnalysis> visionAnalysisOpt = visionAnalysisRepository.findByAnomalyId(eventId);

            if (visionAnalysisOpt.isPresent() && visionAnalysisOpt.get().getImageUrl() != null && !visionAnalysisOpt.get().getImageUrl().isBlank()) {
                VisionAnalysis visionAnalysis = visionAnalysisOpt.get();
                String imageUrl = visionAnalysis.getImageUrl();
                String imageFileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

                try {
                    Map<String, Object> aiResult = aiIntegrationService
                            .requestAnomalyDetail(anomaly.getPowerPlant().getId(), imageFileName)
                            .join();

                    String relativeUrl = (String) aiResult.get("heatmap_url");
                    String absoluteHeatmapUrl = null;
                    if (relativeUrl != null && !relativeUrl.isBlank()) {
                        absoluteHeatmapUrl = aiServerBaseUrl + relativeUrl;
                    }
                    return toDto(anomaly, aiResult, absoluteHeatmapUrl);
                } catch (Exception e) {
                    log.error("AI 분석 요청 중 오류 발생 (eventId: {}): {}", eventId, e.getMessage());
                    return toDto(anomaly, null, null);
                }
            }
        }
        
        return toDto(anomaly, null, null);
    }

    @Transactional
    public AnomalyDto updateStatus(Long eventId, String status) {
        Anomaly anomaly = anomalyRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("이벤트를 찾을 수 없습니다. ID: " + eventId));
        
        anomaly.setStatus(status);
        LocalDateTime now = simulationService.getVirtualCurrentTime();
        anomaly.setUpdatedAt(now);
        if ("RESOLVED".equals(status)) {
            anomaly.setResolvedAt(now);
        }
        
        Anomaly updatedAnomaly = anomalyRepository.save(anomaly);
        return toDto(updatedAnomaly, null, updatedAnomaly.getHeatmapUrl());
    }

    private AnomalyDto toDto(Anomaly anomaly, Map<String, Object> aiResult, String heatmapUrl) {
        String absoluteImageUrl = null;
        if (anomaly.getImageUrl() != null && !anomaly.getImageUrl().isBlank()) {
            absoluteImageUrl = String.format("http://%s:%s%s", serverAddress, serverPort, anomaly.getImageUrl());
        }

        AnomalyDto.AnomalyDtoBuilder builder = AnomalyDto.builder()
                .eventId(anomaly.getId())
                .plantId(anomaly.getPowerPlant().getId())
                .type(anomaly.getType())
                .severity(anomaly.getSeverity())
                .detectedAt(anomaly.getDetectedAt())
                .summary(anomaly.getSummary())
                .status(anomaly.getStatus())
                .imageUrl(absoluteImageUrl)
                .heatmapUrl(heatmapUrl);

        if (aiResult != null) {
            builder.cause((String) aiResult.get("cause"))
                   .recommendedAction((String) aiResult.get("recommendation"));
        } else {
            builder.cause(anomaly.getCause())
                   .recommendedAction(anomaly.getRecommendedAction());
        }
        
        return builder.build();
    }
}
