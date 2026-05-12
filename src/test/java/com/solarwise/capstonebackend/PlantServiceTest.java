package com.solarwise.capstonebackend;

import com.solarwise.capstonebackend.dto.CreatePlantRequest;
import com.solarwise.capstonebackend.dto.PlantResponse;
import com.solarwise.capstonebackend.dto.UpdatePlantRequest;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.entity.User;
import com.solarwise.capstonebackend.exception.BusinessException;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import com.solarwise.capstonebackend.repository.UserRepository;
import com.solarwise.capstonebackend.service.PlantService;
import com.solarwise.capstonebackend.service.SimulationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlantServiceTest {

    @Mock
    private PowerPlantRepository powerPlantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimulationService simulationService;

    @InjectMocks
    private PlantService plantService;

    @Test
    void createPlant_success() {
        User user = User.builder().id(10L).email("owner@solarwise.com").name("manager").build();
        CreatePlantRequest request = CreatePlantRequest.builder()
                .name("iksan-plant-1")
                .location("iksan")
                .capacityKw(120.5)
                .panelCount(320)
                .inverterModel("INV-3000")
                .sensorSerialNumber("SNSR-2026-0001")
                .latitude(35.95)
                .longitude(126.95)
                .build();

        LocalDateTime virtualNow = LocalDateTime.of(2026, 5, 12, 10, 0);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(powerPlantRepository.existsByName("iksan-plant-1")).thenReturn(false);
        when(simulationService.getVirtualCurrentTime()).thenReturn(virtualNow);
        when(powerPlantRepository.save(any(PowerPlant.class))).thenAnswer(invocation -> {
            PowerPlant plant = invocation.getArgument(0);
            plant.setId(99L);
            return plant;
        });

        PlantResponse response = plantService.createPlant(10L, request);

        assertThat(response.getPlantId()).isEqualTo(99L);
        assertThat(response.getName()).isEqualTo("iksan-plant-1");
        assertThat(response.getCapacityKw()).isEqualTo(120.5);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<PowerPlant> captor = ArgumentCaptor.forClass(PowerPlant.class);
        verify(powerPlantRepository).save(captor.capture());
        PowerPlant saved = captor.getValue();
        assertThat(saved.getCreatedAt()).isEqualTo(virtualNow);
        assertThat(saved.getUpdatedAt()).isEqualTo(virtualNow);
        assertThat(saved.getUser().getId()).isEqualTo(10L);
    }

    @Test
    void createPlant_throwsWhenNameDuplicated() {
        User user = User.builder().id(10L).build();
        CreatePlantRequest request = CreatePlantRequest.builder()
                .name("dup-plant")
                .location("seoul")
                .capacityKw(10.0)
                .panelCount(10)
                .inverterModel("INV")
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(powerPlantRepository.existsByName("dup-plant")).thenReturn(true);

        assertThatThrownBy(() -> plantService.createPlant(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 존재하는 발전소 이름");
    }

    @Test
    void createPlant_throwsWhenUserMissing() {
        CreatePlantRequest request = CreatePlantRequest.builder()
                .name("new-plant")
                .location("busan")
                .capacityKw(15.0)
                .panelCount(20)
                .inverterModel("INV")
                .build();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> plantService.createPlant(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    void updatePlant_success() {
        PowerPlant plant = PowerPlant.builder()
                .id(1L)
                .name("plant-old")
                .location("old")
                .capacity(10.0)
                .panelCount(10)
                .status("ACTIVE")
                .active(true)
                .build();

        UpdatePlantRequest request = UpdatePlantRequest.builder()
                .name("plant-new")
                .location("new-location")
                .capacityKw(22.0)
                .panelCount(15)
                .inverterModel("INV-NEW")
                .sensorSerialNumber("SNSR-NEW")
                .latitude(37.1)
                .longitude(127.2)
                .build();

        LocalDateTime virtualNow = LocalDateTime.of(2026, 5, 12, 12, 0);
        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(plant));
        when(powerPlantRepository.existsByNameAndIdNot("plant-new", 1L)).thenReturn(false);
        when(simulationService.getVirtualCurrentTime()).thenReturn(virtualNow);
        when(powerPlantRepository.save(any(PowerPlant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlantResponse response = plantService.updatePlant(1L, 10L, request);

        assertThat(response.getName()).isEqualTo("plant-new");
        assertThat(response.getLocation()).isEqualTo("new-location");
        assertThat(response.getCapacityKw()).isEqualTo(22.0);
        assertThat(plant.getUpdatedAt()).isEqualTo(virtualNow);
    }

    @Test
    void updatePlant_throwsWhenNameDuplicated() {
        PowerPlant plant = PowerPlant.builder().id(1L).build();
        UpdatePlantRequest request = UpdatePlantRequest.builder()
                .name("dup-name")
                .location("x")
                .capacityKw(1.0)
                .panelCount(1)
                .inverterModel("inv")
                .build();

        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(plant));
        when(powerPlantRepository.existsByNameAndIdNot("dup-name", 1L)).thenReturn(true);

        assertThatThrownBy(() -> plantService.updatePlant(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 존재하는 발전소 이름");
    }

    @Test
    void deletePlant_setsInactiveStatus() {
        PowerPlant plant = PowerPlant.builder()
                .id(1L)
                .name("plant-a")
                .status("ACTIVE")
                .active(true)
                .build();
        LocalDateTime virtualNow = LocalDateTime.of(2026, 5, 12, 13, 0);

        when(powerPlantRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(plant));
        when(simulationService.getVirtualCurrentTime()).thenReturn(virtualNow);
        when(powerPlantRepository.save(any(PowerPlant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        plantService.deletePlant(1L, 10L);

        assertThat(plant.getActive()).isFalse();
        assertThat(plant.getStatus()).isEqualTo("INACTIVE");
        assertThat(plant.getUpdatedAt()).isEqualTo(virtualNow);
    }
}
