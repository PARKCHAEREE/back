package com.solarwise.capstonebackend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 시뮬레이션 서비스
 * - 가상 시간 기반의 시뮬레이션 아키텍처를 제공합니다.
 * - 1분마다 가상 시간을 1시간씩 증가시키고, 드론 에러 트리거 시 이미지 이상 탐지를 수행합니다.
 */
@Slf4j
@Service
public class SimulationService {

    // 가상 현재 시간 (초기값: 2026-03-15 13:00:00)
    private LocalDateTime virtualCurrentTime = LocalDateTime.of(2026, 3, 15, 13, 0);

    // 드론 에러 트리거 플래그
    private final AtomicBoolean triggerNextError = new AtomicBoolean(false);

    /**
     * 가상 현재 시간을 반환합니다.
     *
     * @return 가상 현재 시간
     */
    public LocalDateTime getVirtualCurrentTime() {
        return virtualCurrentTime;
    }

    /**
     * 가상 시간을 1시간 앞으로 땡깁니다.
     */
    public void advanceTimeByHour() {
        virtualCurrentTime = virtualCurrentTime.plusHours(1);
        log.info("가상 시간 1시간 전진: {}", virtualCurrentTime);
    }

    /**
     * 드론 에러 트리거를 설정합니다.
     */
    public void triggerDroneError() {
        triggerNextError.set(true);
        log.info("드론 에러 트리거 설정됨");
    }

    /**
     * 1분마다 실행되는 스케줄러
     * - 가상 시간을 1시간 증가
     * - 드론 에러 트리거가 설정된 경우 이미지 이상 탐지 수행
     */
    @Scheduled(fixedRate = 60000) // 1분 = 60000ms
    public void simulateTimeProgression() {
        // 가상 시간 1시간 증가
        advanceTimeByHour();

        // 드론 에러 트리거 확인
        if (triggerNextError.get()) {
            log.info("드론 에러 트리거 활성화: 파손 패널 이미지 분석 수행");
            // TODO: AiIntegrationService.detectVisionAnomaly 호출 (파손 이미지)
            // detectVisionAnomaly(plantId, "PANEL_001", damagedImageBase64);
            triggerNextError.set(false); // 트리거 리셋
        } else {
            log.debug("정상 패널 이미지 분석 수행");
            // TODO: AiIntegrationService.detectVisionAnomaly 호출 (정상 이미지)
            // detectVisionAnomaly(plantId, "PANEL_001", normalImageBase64);
        }
    }
}
