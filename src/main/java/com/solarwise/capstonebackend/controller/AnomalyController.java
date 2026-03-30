package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.AnomalyDto;
import com.solarwise.capstonebackend.service.AnomalyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 이상 탐지 컨트롤러
 * - 이상 탐지 결과 조회 및 XAI 설명 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
@Tag(name = "Anomalies", description = "이상 탐지 관련 API")
public class AnomalyController {

    private final AnomalyService anomalyService;

    /**
     * 발전소별 최근 이상 탐지 조회
     */
    @GetMapping("/power-plant/{powerPlantId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "이상 탐지 조회", description = "특정 발전소의 최근 이상 탐지 결과 조회")
    public ResponseEntity<List<AnomalyDto>> getRecentAnomalies(
            @PathVariable Long powerPlantId,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("이상 탐지 조회: powerPlantId={}, limit={}", powerPlantId, limit);
        List<AnomalyDto> anomalies = anomalyService.getRecentAnomalies(powerPlantId, limit);
        return ResponseEntity.ok(anomalies);
    }

}

