package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.AnomalyDto;
import com.solarwise.capstonebackend.service.AnomalyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 이상 탐지 컨트롤러
 * - 이상 탐지 결과 조회 및 XAI 설명 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/plants/{plantId}/anomalies")
@RequiredArgsConstructor
@Tag(name = "Anomalies", description = "이상 탐지 관련 API")
public class AnomalyController {

    private final AnomalyService anomalyService;

    /**
     * 발전소의 이상 탐지 목록 조회
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "이상 탐지 조회", description = "특정 발전소의 최근 이상 탐지 결과 조회")
    public ResponseEntity<ApiResponse<List<AnomalyDto>>> getAnomalies(
            @PathVariable Long plantId,
            @RequestParam(defaultValue = "10") int limit) {

        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        log.info("이상 탐지 조회: plantId={}, userId={}, limit={}", plantId, userId, limit);

        List<AnomalyDto> anomalies = anomalyService.getRecentAnomalies(plantId, limit);
        return ResponseEntity.ok(ApiResponse.success(anomalies, "이상 이벤트 목록 조회 성공"));
    }

}

