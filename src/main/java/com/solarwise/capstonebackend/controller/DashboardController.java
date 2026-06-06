package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.DashboardSummaryDto;
import com.solarwise.capstonebackend.dto.DashboardTimelineResponse;
import com.solarwise.capstonebackend.dto.MeasurementSeriesDto;
import com.solarwise.capstonebackend.service.DashboardService;
import com.solarwise.capstonebackend.service.MeasurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "Dashboard", description = "대시보드 관련 API")
@RestController
@RequestMapping("/api/v1/plants/{plantId}") // 💡 최종 수정: 공통 경로로 변경
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final MeasurementService measurementService;

    @Operation(summary = "대시보드 요약 조회", description = "현재 발전 상태, 금일 발전량, 최근 이상 여부를 조회합니다.")
    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboardSummary(
            @PathVariable Long plantId,
            @AuthenticationPrincipal Long userId) {
        DashboardSummaryDto summary = dashboardService.getDashboardSummary(plantId, userId);
        return ResponseEntity.ok(ApiResponse.success(summary, "대시보드 요약 조회 성공"));
    }

    @Operation(
            summary = "대시보드 타임라인 조회",
            description = "DB에 적재된 CSV 행을 가상 현재 시간 기준 슬라이딩 윈도우로 조회해 실측/예측/괴리/이상 마커를 반환합니다. 화면 시간은 DB의 시간 컬럼을 그대로 사용합니다."
    )
    @GetMapping("/dashboard/timeline")
    public ResponseEntity<ApiResponse<DashboardTimelineResponse>> getDashboardTimeline(
            @PathVariable Long plantId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "DAY") String range,
            @RequestParam(required = false) Integer futureHours,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        DashboardTimelineResponse timeline = dashboardService.getDashboardTimeline(plantId, userId, range, futureHours, to);
        return ResponseEntity.ok(ApiResponse.success(timeline, "대시보드 타임라인 조회 성공"));
    }

    @Operation(summary = "실측 발전량 시계열 조회", description = "실시간/기간별 발전량 그래프용 데이터를 조회합니다.")
    @GetMapping("/measurements") // 💡 최종 수정: 삭제되었던 API 완벽 복원
    public ResponseEntity<ApiResponse<MeasurementSeriesDto>> getMeasurements(
            @PathVariable Long plantId,
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        MeasurementSeriesDto series = measurementService.getMeasurementSeries(plantId, userId, from, to);
        return ResponseEntity.ok(ApiResponse.success(series, "계측 데이터 조회 성공"));
    }
}
