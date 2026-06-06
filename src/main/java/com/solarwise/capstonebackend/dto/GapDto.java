package com.solarwise.capstonebackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GapDto {
    private LocalDateTime timestamp;
    private Double prediction;
    private Double actual;
}
