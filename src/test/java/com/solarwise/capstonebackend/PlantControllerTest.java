package com.solarwise.capstonebackend;

import com.solarwise.capstonebackend.controller.PlantController;
import com.solarwise.capstonebackend.dto.CreatePlantRequest;
import com.solarwise.capstonebackend.dto.PlantResponse;
import com.solarwise.capstonebackend.dto.UpdatePlantRequest;
import com.solarwise.capstonebackend.service.PlantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlantControllerTest {

    @Mock
    private PlantService plantService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PlantController plantController;

    @Test
    void createPlant_returnsCreated() {
        CreatePlantRequest request = CreatePlantRequest.builder()
                .name("iksan-1")
                .location("iksan")
                .capacityKw(120.5)
                .panelCount(320)
                .inverterModel("INV-3000")
                .build();
        PlantResponse response = PlantResponse.builder().plantId(1L).name("iksan-1").build();

        when(authentication.getPrincipal()).thenReturn("10");
        when(plantService.createPlant(10L, request)).thenReturn(response);

        var result = plantController.createPlant(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData().getPlantId()).isEqualTo(1L);
        verify(plantService).createPlant(10L, request);
    }

    @Test
    void getPlants_returnsOk() {
        PlantResponse plant = PlantResponse.builder().plantId(1L).name("iksan-1").build();
        when(authentication.getPrincipal()).thenReturn("10");
        when(plantService.getPlantsByUser(10L)).thenReturn(List.of(plant));

        var result = plantController.getPlants(authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData()).hasSize(1);
        verify(plantService).getPlantsByUser(10L);
    }

    @Test
    void updatePlant_returnsOk() {
        UpdatePlantRequest request = UpdatePlantRequest.builder()
                .name("iksan-1-updated")
                .location("iksan")
                .capacityKw(130.0)
                .panelCount(340)
                .inverterModel("INV-3500")
                .build();
        PlantResponse response = PlantResponse.builder().plantId(1L).name("iksan-1-updated").build();

        when(authentication.getPrincipal()).thenReturn("10");
        when(plantService.updatePlant(1L, 10L, request)).thenReturn(response);

        var result = plantController.updatePlant(authentication, 1L, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData().getName()).isEqualTo("iksan-1-updated");
        verify(plantService).updatePlant(1L, 10L, request);
    }

    @Test
    void deletePlant_returnsOk() {
        when(authentication.getPrincipal()).thenReturn("10");

        var result = plantController.deletePlant(authentication, 1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().isSuccess()).isTrue();
        verify(plantService).deletePlant(1L, 10L);
    }
}

