package com.solarwise.capstonebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSettingDto {
    private boolean emailEnabled;
    private boolean smsEnabled;
    private String minimumSeverity;
}
