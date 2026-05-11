package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.DashboardSummaryDto;
import com.solarwise.capstonebackend.dto.MeasurementCsvUploadResult;
import com.solarwise.capstonebackend.dto.MeasurementSeriesDto;
import com.solarwise.capstonebackend.service.MeasurementService;
import com.solarwise.capstonebackend.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 대시보드 컨트롤러
 * - 발전소 대시보드 조회 및 측정 데이터 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/plants/{plantId}")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "대시보드 및 측정 데이터 관련 API")
public class DashboardController {

    private final MeasurementService measurementService;
    private final SimulationService simulationService;

    /**
     * 대시보드 요약 조회
     */
    @GetMapping("/dashboard/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "대시보드 요약 조회", description = "발전소의 현재 상태 및 오늘의 발전량 조회")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboardSummary(
            @PathVariable Long plantId) {

        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        log.info("대시보드 요약 조회: plantId={}, userId={}", plantId, userId);

        DashboardSummaryDto response = measurementService.getDashboardSummary(plantId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success(response, "대시보드 요약 조회 성공"));
    }

    /**
     * 실측 CSV 업로드
     */
    @PostMapping(value = "/measurements/upload-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "실측 CSV 업로드", description = "자문가 제공 실측 CSV를 시계열 계측 데이터로 저장")
    public ResponseEntity<ApiResponse<MeasurementCsvUploadResult>> uploadMeasurementsCsv(
            @PathVariable Long plantId,
            @RequestPart("file") MultipartFile file) {

        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        log.info("실측 CSV 업로드: plantId={}, userId={}, file={}", plantId, userId, file.getOriginalFilename());

        MeasurementCsvUploadResult response = measurementService.uploadMeasurementCsv(
                plantId, Long.parseLong(userId), file);
        return ResponseEntity.ok(ApiResponse.success(response, "실측 CSV 업로드 성공"));
    }

    /**
     * 시계열 측정 데이터 조회
     */
    @GetMapping("/measurements")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "측정 데이터 시계열 조회", description = "기간별 발전량 및 기상 데이터 조회")
    public ResponseEntity<ApiResponse<MeasurementSeriesDto>> getMeasurements(
            @PathVariable Long plantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        // 가상 시간 기준으로 기본값 설정 (최근 24시간)
        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        LocalDateTime endTime = to != null ? to : virtualNow;
        LocalDateTime startTime = from != null ? from : endTime.minusHours(24);

        log.info("측정 데이터 조회: plantId={}, userId={}, from={}, to={}", plantId, userId, startTime, endTime);

        MeasurementSeriesDto response = measurementService.getMeasurementSeries(
                plantId, Long.parseLong(userId), startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success(response, "계측 데이터 조회 성공"));
    }

}

