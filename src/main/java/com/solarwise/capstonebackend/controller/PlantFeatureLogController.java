package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.PlantFeatureLogSeriesDto;
import com.solarwise.capstonebackend.service.PlantFeatureLogService;
import com.solarwise.capstonebackend.service.SimulationService;
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
 *
 * <p>
 * AI 팀의 전처리 CSV 데이터(wooyang_merged_result.csv)를 기반으로 한
 * PlantFeatureLog 통합 엔티티에 대한 조회 API를 제공합니다.
 * </p>
 *
 * <h3>가상 시간 시뮬레이션 적용</h3>
 * 모든 조회 API는 실제 시스템 시간이 아닌,
 * {@link SimulationService#getVirtualCurrentTime()}에서 반환하는 가상 시간을 기준으로 동작합니다.
 *
 * @see com.solarwise.capstonebackend.entity.PlantFeatureLog
 * @see SimulationService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/plants/{plantId}/feature-logs")
@RequiredArgsConstructor
@Tag(name = "Plant Feature Logs",
     description = "발전소 원천 데이터 조회 (CSV 7개 컬럼: TIME, ACTUAL, PREDICTION, TEMP, HUMI, CLOU, IRRADIANCE) - 가상 시간 기반")
public class PlantFeatureLogController {

    private final PlantFeatureLogService plantFeatureLogService;
    private final SimulationService simulationService;

    /**
     * 발전소의 피처 로그 전체 건수 조회
     *
     * @param plantId 발전소 ID
     * @return 피처 로그 총 건수
     */
    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "피처 로그 건수 조회",
        description = "발전소별 plant_feature_logs 총 건수 조회 (자문가 원천 데이터)"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeatureLogCount(
            @PathVariable Long plantId) {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        long count = plantFeatureLogService.getFeatureLogCount(plantId, Long.parseLong(userId));

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("plantId", plantId, "count", count),
                "피처 로그 건수 조회 성공"
        ));
    }

    /**
     * 발전소의 최신 피처 로그 N건 조회 (가상 시간 기준)
     *
     * <p>
     * 가상 시간 기준 "최신" 데이터 N건을 반환합니다.
     * 예: 가상 시간 2026-04-26 18:00, limit=24 → 최근 24건 (그 이전 시점)
     * </p>
     *
     * @param plantId 발전소 ID
     * @param limit 조회할 최대 건수 (기본값: 24, 최대: 500)
     * @return 최신 N건의 피처 로그 시계열
     */
    @GetMapping("/latest")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "최신 피처 로그 조회 (가상 시간 기준)",
        description = "발전소별 가상 시간 기준 최신 피처 로그 N건 조회"
    )
    public ResponseEntity<ApiResponse<PlantFeatureLogSeriesDto>> getLatestFeatureLogs(
            @PathVariable Long plantId,
            @RequestParam(defaultValue = "24") int limit) {

        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();

        log.info("최신 피처 로그 조회: plantId={}, userId={}, limit={}, virtualNow={}",
                plantId, userId, limit, virtualNow);

        PlantFeatureLogSeriesDto response = plantFeatureLogService.getLatestFeatureLogs(
                plantId, Long.parseLong(userId), limit);

        return ResponseEntity.ok(ApiResponse.success(response, "최신 피처 로그 조회 성공"));
    }

    /**
     * 발전소의 기간별 피처 로그 조회 (가상 시간 기준)
     *
     * <p>
     * 주어진 기간 내의 피처 로그를 조회합니다.
     * 파라미터가 없으면 가상 시간 기준 과거 24시간 데이터를 반환합니다.
     * </p>
     *
     * <h3>가상 시간 동기화</h3>
     * - 파라미터 from, to 없음 → 가상 시간 - 24h ~ 가상 시간 범위 조회
     * - from 또는 to만 제공 → 나머지는 가상 시간 기준 자동 계산
     * - 양쪽 다 제공 → 정확히 [from, to] 범위 조회
     *
     * @param plantId 발전소 ID
     * @param from 조회 시작 시간 (ISO 8601, 선택사항)
     * @param to 조회 종료 시간 (ISO 8601, 선택사항)
     * @return 기간별 피처 로그 시계열
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "기간별 피처 로그 조회 (가상 시간 기반)",
        description = "발전소별 기간(from~to) 피처 로그 조회. 파라미터 없으면 가상 시간 기준 과거 24시간"
    )
    public ResponseEntity<ApiResponse<PlantFeatureLogSeriesDto>> getFeatureLogSeries(
            @PathVariable Long plantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 🔑 가상 시간 기준으로 시간 범위 설정
        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        LocalDateTime endTime = to != null ? to : virtualNow;
        LocalDateTime startTime = from != null ? from : endTime.minusHours(24);

        log.info("기간별 피처 로그 조회: plantId={}, userId={}, from={}, to={}, virtualNow={}",
                plantId, userId, startTime, endTime, virtualNow);

        PlantFeatureLogSeriesDto response = plantFeatureLogService.getFeatureLogSeries(
                plantId,
                Long.parseLong(userId),
                startTime,
                endTime
        );

        return ResponseEntity.ok(ApiResponse.success(response, "기간별 피처 로그 조회 성공"));
    }
}
