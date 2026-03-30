package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.AnomalyDto;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

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

    /**
     * 최근 이상 탐지 조회
     */
    public List<AnomalyDto> getRecentAnomalies(Long powerPlantId, int limit) {
        return anomalyRepository.findByPowerPlantIdOrderByDetectedAtDesc(powerPlantId)
                .stream()
                .limit(limit)
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    /**
     * 엔티티를 DTO로 변환
     */
    private AnomalyDto entityToDto(com.solarwise.capstonebackend.entity.Anomaly anomaly) {
        return AnomalyDto.builder()
                .id(anomaly.getId())
                .powerPlantId(anomaly.getPowerPlant().getId())
                .type(anomaly.getType())
                .description(anomaly.getDescription())
                .severity(anomaly.getSeverity())
                .xaiExplanation(anomaly.getXaiExplanation())
                .status(anomaly.getStatus())
                .detectedAt(anomaly.getDetectedAt())
                .resolvedAt(anomaly.getResolvedAt())
                .build();
    }

}

