package com.solarwise.capstonebackend.dto.ai;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AiApiResponse<T> {
    private String status;
    private int code;
    private T data;
    private String message; // For error responses
}