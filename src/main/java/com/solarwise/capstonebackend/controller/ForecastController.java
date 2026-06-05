package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.ForecastResponseDto;
import com.solarwise.capstonebackend.service.ForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "Forecasts", description = "발전량 예측 API")
@RestController
@RequestMapping("/api/v1/plants/{plantId}/forecasts")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastService forecastService;

    @Operation(summary = "예측 발전량 조회", description = "향후 2~3일의 예측 발전량 데이터를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<ForecastResponseDto>> getForecasts(
            @PathVariable Long plantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        ForecastResponseDto response = forecastService.getForecasts(plantId, from, to);
        return ResponseEntity.ok(ApiResponse.success(response, "예측 데이터 조회 성공"));
    }

    // TODO: /explanations API 구현 필요
}
