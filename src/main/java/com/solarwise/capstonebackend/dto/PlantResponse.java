package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlantResponse {
    private Long plantId;
    private String name;
    private String location;
    private Double capacityKw;
    private String status;
    private String inverterModel;
    private String sensorSerialNumber;
}
