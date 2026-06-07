package com.solarwise.capstonebackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateAnomalyStatusRequest {
    private String status;
}
