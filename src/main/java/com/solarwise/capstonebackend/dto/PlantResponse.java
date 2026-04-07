package com.solarwise.capstonebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 발전소 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantResponse {

    private Long plantId;
    private String name;
    private String location;
    private Double capacityKw;
    private String status;
    private String inverterModel;
    private String sensorSerialNumber;

}

