package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.PlantFeatureLogSeriesDto;
import com.solarwise.capstonebackend.service.PlantFeatureLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 발전소 고급 피처 로그 조회 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/plants/{plantId}/feature-logs")
@RequiredArgsConstructor
@Tag(name = "Plant Feature Logs", description = "자문가 원천/피처 로그 조회 API")
public class PlantFeatureLogController {

    private final PlantFeatureLogService plantFeatureLogService;

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "피처 로그 건수 조회", description = "발전소별 plant_feature_logs 총 건수 조회")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeatureLogCount(@PathVariable Long plantId) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        long count = plantFeatureLogService.getFeatureLogCount(plantId, Long.parseLong(userId));

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("plantId", plantId, "count", count),
                "피처 로그 건수 조회 성공"
        ));
    }

    @GetMapping("/latest")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "최신 피처 로그 조회", description = "발전소별 최신 피처 로그 N건 조회")
    public ResponseEntity<ApiResponse<PlantFeatureLogSeriesDto>> getLatestFeatureLogs(
            @PathVariable Long plantId,
            @RequestParam(defaultValue = "24") int limit) {

        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        PlantFeatureLogSeriesDto response = plantFeatureLogService.getLatestFeatureLogs(plantId, Long.parseLong(userId), limit);

        return ResponseEntity.ok(ApiResponse.success(response, "최신 피처 로그 조회 성공"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "기간별 피처 로그 조회", description = "발전소별 기간(from~to) 피처 로그 조회")
    public ResponseEntity<ApiResponse<PlantFeatureLogSeriesDto>> getFeatureLogSeries(
            @PathVariable Long plantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        LocalDateTime endTime = to != null ? to : LocalDateTime.now();
        LocalDateTime startTime = from != null ? from : endTime.minusHours(24);

        PlantFeatureLogSeriesDto response = plantFeatureLogService.getFeatureLogSeries(
                plantId,
                Long.parseLong(userId),
                startTime,
                endTime
        );

        return ResponseEntity.ok(ApiResponse.success(response, "기간별 피처 로그 조회 성공"));
    }
}

