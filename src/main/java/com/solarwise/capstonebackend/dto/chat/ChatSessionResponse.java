package com.solarwise.capstonebackend.dto.chat;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSessionResponse {

    private Long sessionId;

    private String sessionTitle;

    private Long plantId;

    private LocalDateTime updatedAt;
}

