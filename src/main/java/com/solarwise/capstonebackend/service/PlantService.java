package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.CreatePlantRequest;
import com.solarwise.capstonebackend.dto.PlantResponse;
import com.solarwise.capstonebackend.dto.UpdatePlantRequest;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.entity.User;
import com.solarwise.capstonebackend.exception.BusinessException;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlantService {

    private final PowerPlantRepository powerPlantRepository;
    private final UserRepository userRepository;
    private final SimulationService simulationService;

    @Transactional
    public PlantResponse createPlant(Long userId, CreatePlantRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        if (powerPlantRepository.existsByName(request.getName())) {
            throw new BusinessException("이미 존재하는 발전소 이름입니다.", HttpStatus.CONFLICT);
        }

        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        PowerPlant plant = PowerPlant.builder()
                .name(request.getName())
                .location(request.getLocation())
                .capacity(request.getCapacityKw())
                .panelCount(request.getPanelCount())
                .inverterModel(request.getInverterModel())
                .sensorSerialNumber(request.getSensorSerialNumber())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status("ACTIVE")
                .user(user)
                .createdAt(virtualNow)
                .updatedAt(virtualNow)
                .build();

        PowerPlant savedPlant = powerPlantRepository.save(plant);
        return entityToResponse(savedPlant);
    }

    @Transactional
    public PlantResponse updatePlant(Long plantId, Long userId, UpdatePlantRequest request) {
        PowerPlant plant = powerPlantRepository.findByIdAndUserId(plantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        if (powerPlantRepository.existsByNameAndIdNot(request.getName(), plantId)) {
            throw new BusinessException("이미 존재하는 발전소 이름입니다.", HttpStatus.CONFLICT);
        }

        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        plant.setName(request.getName());
        plant.setLocation(request.getLocation());
        plant.setCapacity(request.getCapacityKw());
        plant.setPanelCount(request.getPanelCount());
        plant.setInverterModel(request.getInverterModel());
        plant.setSensorSerialNumber(request.getSensorSerialNumber());
        plant.setLatitude(request.getLatitude());
        plant.setLongitude(request.getLongitude());
        plant.setUpdatedAt(virtualNow);

        PowerPlant savedPlant = powerPlantRepository.save(plant);
        return entityToResponse(savedPlant);
    }

    @Transactional
    public void deletePlant(Long plantId, Long userId) {
        PowerPlant plant = powerPlantRepository.findByIdAndUserId(plantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        LocalDateTime virtualNow = simulationService.getVirtualCurrentTime();
        plant.setActive(false);
        plant.setStatus("INACTIVE");
        plant.setUpdatedAt(virtualNow);

        powerPlantRepository.save(plant);
    }

    @Transactional(readOnly = true)
    public List<PlantResponse> getPlantsByUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        List<PowerPlant> plants = powerPlantRepository.findByUserId(userId);
        return plants.stream()
                .map(this::entityToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlantResponse getPlantDetail(Long plantId, Long userId) {
        PowerPlant plant = powerPlantRepository.findByIdAndUserId(plantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("발전소를 찾을 수 없습니다."));

        return entityToResponse(plant);
    }

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
