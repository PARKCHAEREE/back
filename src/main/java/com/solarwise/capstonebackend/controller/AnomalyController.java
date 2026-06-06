package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.AnomalyDto;
import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.service.AnomalyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Anomalies", description = "이상 감지 이벤트 API")
@RestController
@RequestMapping("/api/v1/plants/{plantId}/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyService anomalyService;

    @Operation(summary = "이상 이벤트 상세 조회 (AI 분석 포함)", description = "특정 이상 이벤트에 대한 상세 정보와 AI의 원인 분석, 히트맵 URL을 함께 조회합니다.")
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<AnomalyDto>> getAnomalyDetail(
            @PathVariable Long plantId,
            @PathVariable Long eventId) {
        // plantId는 경로에 있지만, eventId로 Anomaly를 찾으므로 직접 사용하지는 않음
        AnomalyDto anomalyDetail = anomalyService.getAnomalyDetailWithAi(eventId);
        return ResponseEntity.ok(ApiResponse.success(anomalyDetail, "이상 이벤트 상세 조회 성공"));
    }
    // 💡 목록 조회를 위한 메서드 추가 (경로 끝에 /{eventId}가 없음!)
    @Operation(summary = "이상 이벤트 목록 조회", description = "해당 발전소의 전체 이상 감지 이벤트 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AnomalyDto>>> getAnomalyList(@PathVariable Long plantId) {
        // 서비스에서 목록을 가져오는 로직을 호출 (예: anomalyService.getAnomalyList(plantId))
        List<AnomalyDto> anomalyList = anomalyService.getAnomalyList(plantId);
        return ResponseEntity.ok(ApiResponse.success(anomalyList, "이상 이벤트 목록 조회 성공"));
    }
}
