package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.CreatePlantRequest;
import com.solarwise.capstonebackend.dto.PlantResponse;
import com.solarwise.capstonebackend.dto.UpdatePlantRequest;
import com.solarwise.capstonebackend.service.PlantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
     * 발전소 등록
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "발전소 등록", description = "로그인한 사용자의 발전소를 신규 등록")
    public ResponseEntity<ApiResponse<PlantResponse>> createPlant(
            Authentication authentication,
            @Valid @RequestBody CreatePlantRequest request) {
        Long userId = parseUserId(authentication);

        log.info("발전소 등록 요청: userId={}, name={}", userId, request.getName());

        PlantResponse response = plantService.createPlant(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "발전소가 등록되었습니다."));
    }

    /**
     * 사용자의 발전소 목록 조회
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "발전소 목록 조회", description = "로그인한 사용자의 발전소 목록 조회")
    public ResponseEntity<ApiResponse<List<PlantResponse>>> getPlants(Authentication authentication) {
        Long userId = parseUserId(authentication);

        log.info("발전소 목록 조회: userId={}", userId);

        List<PlantResponse> plants = plantService.getPlantsByUser(userId);
        return ResponseEntity.ok(ApiResponse.success(plants, "발전소 목록 조회 성공"));
    }

    /**
     * 발전소 상세 조회
     */
    @GetMapping("/{plantId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "발전소 상세 조회", description = "선택한 발전소의 상세 정보 조회")
    public ResponseEntity<ApiResponse<PlantResponse>> getPlantDetail(
            Authentication authentication,
            @PathVariable Long plantId) {
        Long userId = parseUserId(authentication);

        log.info("발전소 상세 조회: plantId={}, userId={}", plantId, userId);

        PlantResponse plant = plantService.getPlantDetail(plantId, userId);
        return ResponseEntity.ok(ApiResponse.success(plant, "발전소 상세 조회 성공"));
    }

    /**
     * 발전소 정보 수정
     */
    @PutMapping("/{plantId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "발전소 정보 수정", description = "로그인한 사용자의 발전소 정보를 수정")
    public ResponseEntity<ApiResponse<PlantResponse>> updatePlant(
            Authentication authentication,
            @PathVariable Long plantId,
            @Valid @RequestBody UpdatePlantRequest request) {
        Long userId = parseUserId(authentication);

        log.info("발전소 수정 요청: plantId={}, userId={}", plantId, userId);

        PlantResponse response = plantService.updatePlant(plantId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "발전소 정보가 수정되었습니다."));
    }

    /**
     * 발전소 삭제 (소프트 삭제)
     */
    @DeleteMapping("/{plantId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "발전소 삭제", description = "로그인한 사용자의 발전소를 비활성화 처리")
    public ResponseEntity<ApiResponse<Void>> deletePlant(
            Authentication authentication,
            @PathVariable Long plantId) {
        Long userId = parseUserId(authentication);

        log.info("발전소 삭제 요청: plantId={}, userId={}", plantId, userId);

        plantService.deletePlant(plantId, userId);
        return ResponseEntity.ok(ApiResponse.success("발전소가 삭제되었습니다."));
    }

    private Long parseUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }
        return Long.parseLong(authentication.getPrincipal().toString());
    }

}

