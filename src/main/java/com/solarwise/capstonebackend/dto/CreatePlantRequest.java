package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 발전소 등록 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlantRequest {

    @NotBlank(message = "name은 필수입니다.")
    private String name;

    @NotBlank(message = "location은 필수입니다.")
    private String location;

    @NotNull(message = "capacityKw는 필수입니다.")
    @Positive(message = "capacityKw는 0보다 커야 합니다.")
    @JsonAlias("capacity")
    private Double capacityKw;

    @NotNull(message = "panelCount는 필수입니다.")
    @Positive(message = "panelCount는 0보다 커야 합니다.")
    private Integer panelCount;

    @NotBlank(message = "inverterModel은 필수입니다.")
    private String inverterModel;

    private String sensorSerialNumber;

    private Double latitude;

    private Double longitude;
}

