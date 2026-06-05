package com.solarwise.capstonebackend.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageResponse {
    private String answer;
    private List<String> references;
}
