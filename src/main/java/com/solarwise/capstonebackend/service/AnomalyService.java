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

        // 'VISION' 타입 이상 이벤트에 대해서만 AI 분석을 시도
        if ("VISION".equals(anomaly.getType())) {
            Optional<VisionAnalysis> visionAnalysisOpt = visionAnalysisRepository.findByAnomalyId(eventId);

            // VisionAnalysis 데이터가 존재하고, 이미지 URL이 있을 경우에만 AI 분석 요청
            if (visionAnalysisOpt.isPresent() && visionAnalysisOpt.get().getImageUrl() != null) {
                VisionAnalysis visionAnalysis = visionAnalysisOpt.get();
                String imageUrl = visionAnalysis.getImageUrl();
                
                // 💡 경로 문제 해결: 순수한 파일 이름만 추출 (예: /images/crack.jpg -> crack.jpg)
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
                    // AI 분석 실패 시, DB에 저장된 정보만으로 응답
                    return toDto(anomaly, null, null);
                }
            }
        }
        
        // VISION 타입이 아니거나, VisionAnalysis 데이터가 없는 경우 DB 정보만 반환
        return toDto(anomaly, null, null);
    }

    private AnomalyDto toDto(Anomaly anomaly, Map<String, Object> aiResult, String heatmapUrl) {
        AnomalyDto.AnomalyDtoBuilder builder = AnomalyDto.builder()
                .eventId(anomaly.getId())
                .plantId(anomaly.getPowerPlant().getId())
                .type(anomaly.getType())
                .severity(anomaly.getSeverity())
                .detectedAt(anomaly.getDetectedAt())
                .summary(anomaly.getSummary())
                .status(anomaly.getStatus());

        if (aiResult != null) {
            builder.cause((String) aiResult.get("cause"))
                   .recommendedAction((String) aiResult.get("recommendation"));
        } else {
            // AI 분석 결과가 없으면, DB에 저장된 값을 사용
            builder.cause(anomaly.getCause())
                   .recommendedAction(anomaly.getRecommendedAction());
        }
        
        // 프론트엔드 구현 전까지 히트맵 URL은 null로 유지
        // builder.heatmapUrl(heatmapUrl);
        
        return builder.build();
    }
}
