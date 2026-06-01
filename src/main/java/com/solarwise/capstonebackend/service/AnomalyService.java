package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.AnomalyDto;
import com.solarwise.capstonebackend.dto.UpdateAnomalyStatusResponse;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.exception.BusinessException;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 이상 탐지 서비스
 * - AI 결과 기반 이상 탐지 및 관리
 * - XAI 설명 매핑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyService {

    private final AnomalyRepository anomalyRepository;
    private final PowerPlantRepository powerPlantRepository;
    private final SimulationService simulationService;

    /**
     * 최근 이상 탐지 조회
     */
    public List<AnomalyDto> getRecentAnomalies(Long powerPlantId, Long userId, int limit) {
        validatePlantAccess(powerPlantId, userId);

        return anomalyRepository.findByPowerPlantIdOrderByDetectedAtDesc(powerPlantId)
                .stream()
                .limit(limit)
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    /**
     * 최근 이상 탐지 조회 (내부 서비스용)
     */
    public List<AnomalyDto> getRecentAnomalies(Long powerPlantId, int limit) {
        return anomalyRepository.findByPowerPlantIdOrderByDetectedAtDesc(powerPlantId)
                .stream()
                .limit(limit)
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    /**
     * 이상 이벤트 상세 조회
     */
    public AnomalyDto getAnomalyDetail(Long powerPlantId, Long eventId, Long userId) {
        validatePlantAccess(powerPlantId, userId);

        Anomaly anomaly = anomalyRepository.findByIdAndPowerPlantId(eventId, powerPlantId)
                .orElseThrow(() -> new ResourceNotFoundException("이상 이벤트를 찾을 수 없습니다."));

        return entityToDto(anomaly);
    }

    /**
     * 이상 이벤트 상태 변경
     */
    @Transactional
    public UpdateAnomalyStatusResponse updateAnomalyStatus(Long powerPlantId, Long eventId, Long userId, String status) {
        validatePlantAccess(powerPlantId, userId);

        Anomaly anomaly = anomalyRepository.findByIdAndPowerPlantId(eventId, powerPlantId)
                .orElseThrow(() -> new ResourceNotFoundException("이상 이벤트를 찾을 수 없습니다."));

        String normalizedStatus = normalizeStatus(status);
        validateSupportedStatus(normalizedStatus);

        anomaly.setStatus(normalizedStatus);
        if ("RESOLVED".equals(normalizedStatus)) {
            // 테스트 환경에서는 simulationService가 Mockito에 의해 주입되지 않을 수 있으므로
            // NullPointerException을 피하기 위해 null 검사 후 대체값을 사용합니다.
            LocalDateTime resolvedAt = simulationService != null ? simulationService.getVirtualCurrentTime() : LocalDateTime.now();
            anomaly.setResolvedAt(resolvedAt);
        } else {
            anomaly.setResolvedAt(null);
        }

        Anomaly updatedAnomaly = anomalyRepository.save(anomaly);
        return UpdateAnomalyStatusResponse.builder()
                .eventId(updatedAnomaly.getId())
                .status(normalizeStatus(updatedAnomaly.getStatus()))
                .build();
    }

    /**
     * 엔티티를 DTO로 변환
     */
    private AnomalyDto entityToDto(Anomaly anomaly) {
        return AnomalyDto.builder()
                .eventId(anomaly.getId())
                .type(anomaly.getType())
                .severity(anomaly.getSeverity())
                .detectedAt(anomaly.getDetectedAt())
                .summary(anomaly.getSummary())
                .status(normalizeStatus(anomaly.getStatus()))
                .cause(anomaly.getCause())
                .recommendedAction(anomaly.getRecommendedAction())
                .xaiExplanation(anomaly.getXaiExplanation())
                .build();
    }

    private PowerPlant validatePlantAccess(Long powerPlantId, Long userId) {
        return powerPlantRepository.findByIdAndUserId(powerPlantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));
    }

    private void validateSupportedStatus(String status) {
        if (!"OPEN".equals(status) && !"ACKNOWLEDGED".equals(status) && !"RESOLVED".equals(status)) {
            throw new BusinessException("지원하지 않는 이상 이벤트 상태입니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank() || "DETECTED".equals(status)) {
            return "OPEN";
        }
        return status;
    }

}

