package com.solarwise.capstonebackend.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatSessionResponse {
    private String sessionId;
    private String welcomeMessage;
}
