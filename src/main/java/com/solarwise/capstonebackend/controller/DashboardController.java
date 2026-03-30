package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.DashboardResponse;
import com.solarwise.capstonebackend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 대시보드 컨트롤러
 * - 발전소 대시보드 조회 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "대시보드 조회 관련 API")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 발전소 대시보드 조회
     */
    @GetMapping("/power-plant/{powerPlantId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "대시보드 조회", description = "특정 발전소의 대시보드 데이터 조회 (집계된 발전량, 이상 탐지)")
    public ResponseEntity<DashboardResponse> getDashboard(
            @PathVariable Long powerPlantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("대시보드 조회: powerPlantId={}", powerPlantId);

        // 기본값: 최근 24시간
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        LocalDateTime start = startTime != null ? startTime : end.minusHours(24);

        DashboardResponse response = dashboardService.getDashboard(powerPlantId, start, end);
        return ResponseEntity.ok(response);
    }

}

