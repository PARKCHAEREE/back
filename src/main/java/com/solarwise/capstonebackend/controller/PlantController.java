package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.PlantResponse;
import com.solarwise.capstonebackend.service.PlantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 발전소 컨트롤러
 * - 발전소 목록 및 상세 조회 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/plants")
@RequiredArgsConstructor
@Tag(name = "Plants", description = "발전소 관련 API")
public class PlantController {

    private final PlantService plantService;

    /**
     * 사용자의 발전소 목록 조회
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "발전소 목록 조회", description = "로그인한 사용자의 발전소 목록 조회")
    public ResponseEntity<ApiResponse<List<PlantResponse>>> getPlants() {
        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        log.info("발전소 목록 조회: userId={}", userId);

        List<PlantResponse> plants = plantService.getPlantsByUser(Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success(plants, "발전소 목록 조회 성공"));
    }

    /**
     * 발전소 상세 조회
     */
    @GetMapping("/{plantId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "발전소 상세 조회", description = "선택한 발전소의 상세 정보 조회")
    public ResponseEntity<ApiResponse<PlantResponse>> getPlantDetail(@PathVariable Long plantId) {
        String userId = (String) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        log.info("발전소 상세 조회: plantId={}, userId={}", plantId, userId);

        PlantResponse plant = plantService.getPlantDetail(plantId, Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success(plant, "발전소 상세 조회 성공"));
    }

}

