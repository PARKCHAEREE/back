package com.solarwise.capstonebackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlantResponse {

    @JsonProperty("plantId")
    private Long plantId;

    private String name;
    private String location;

    @JsonProperty("capacity") // 요구사항 2: JSON 키값을 "capacity"로 변경
    private Double capacityKw;

    private String status;
    private String inverterModel;
    private String sensorSerialNumber;
}
