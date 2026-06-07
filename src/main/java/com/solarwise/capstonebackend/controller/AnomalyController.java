package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.AnomalyDto;
import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.UpdateAnomalyStatusRequest;
import com.solarwise.capstonebackend.service.AnomalyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Anomalies", description = "이상 감지 이벤트 API")
@RestController
@RequestMapping("/api/v1/plants/{plantId}/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyService anomalyService;

    @Operation(summary = "이상 이벤트 목록 조회", description = "해당 발전소의 전체 이상 감지 이벤트 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AnomalyDto>>> getAnomalyList(@PathVariable Long plantId) {
        List<AnomalyDto> anomalyList = anomalyService.getAnomalyList(plantId);
        return ResponseEntity.ok(ApiResponse.success(anomalyList, "이상 이벤트 목록 조회 성공"));
    }

    @Operation(summary = "이상 이벤트 상세 조회 (AI 분석 포함)", description = "특정 이상 이벤트에 대한 상세 정보와 AI의 원인 분석을 함께 조회합니다.")
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<AnomalyDto>> getAnomalyDetail(
            @PathVariable Long plantId,
            @PathVariable Long eventId) {
        AnomalyDto anomalyDetail = anomalyService.getAnomalyDetailWithAi(eventId);
        return ResponseEntity.ok(ApiResponse.success(anomalyDetail, "이상 이벤트 상세 조회 성공"));
    }

    @Operation(summary = "이상 이벤트 상태 변경", description = "프론트엔드에서 확인 완료/해결 처리합니다.")
    @PatchMapping("/{eventId}/status") // 💡 최종 수정: 사용자의 지시에 따라 @PatchMapping으로 복원
    public ResponseEntity<ApiResponse<AnomalyDto>> updateAnomalyStatus(
            @PathVariable Long plantId,
            @PathVariable Long eventId,
            @RequestBody UpdateAnomalyStatusRequest request) {
        AnomalyDto result = anomalyService.updateStatus(eventId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success(result, "이상 이벤트 상태가 변경되었습니다."));
    }
}
