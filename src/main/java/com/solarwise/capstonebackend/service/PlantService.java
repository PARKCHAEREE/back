package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.PlantResponse;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.entity.User;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 발전소 서비스
 * - 발전소 조회 및 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlantService {

    private final PowerPlantRepository powerPlantRepository;
    private final UserRepository userRepository;

    /**
     * 사용자의 모든 발전소 조회
     */
    public List<PlantResponse> getPlantsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        List<PowerPlant> plants = powerPlantRepository.findByUserId(userId);
        return plants.stream()
                .map(this::entityToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 발전소 상세 조회 (사용자 권한 확인)
     */
    public PlantResponse getPlantDetail(Long plantId, Long userId) {
        PowerPlant plant = powerPlantRepository.findByIdAndUserId(plantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        return entityToResponse(plant);
    }

    /**
     * 엔티티를 응답 DTO로 변환
     */
    private PlantResponse entityToResponse(PowerPlant plant) {
        return PlantResponse.builder()
                .plantId(plant.getId())
                .name(plant.getName())
                .location(plant.getLocation())
                .capacityKw(plant.getCapacity())
                .status(plant.getStatus())
                .inverterModel(plant.getInverterModel())
                .sensorSerialNumber(plant.getSensorSerialNumber())
                .build();
    }

}

