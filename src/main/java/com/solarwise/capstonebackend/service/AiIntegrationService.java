package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.entity.EnergyLog;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.repository.EnergyLogRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    /**
     * AI 서버로 예측 요청 (비동기)
     * TODO: 실제 AI 서버 URL 및 요청/응답 포맷 정의 필요
     */
    public void requestPredictionFromAi(Long powerPlantId) {
        PowerPlant plant = powerPlantRepository.findById(powerPlantId)
                .orElseThrow(() -> new IllegalArgumentException("발전소를 찾을 수 없습니다."));

        log.info("AI 서버로 예측 요청: powerPlantId={}", powerPlantId);
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

        log.info("예측 결과 저장: energyLogId={}, predictedGeneration={}", energyLogId, predictedValue);
    }

}


