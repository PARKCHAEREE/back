package com.solarwise.capstonebackend.dto.chat;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {

    private Long messageId;

    private String senderRole;

    private String content;

    private String imageUrl;

    private LocalDateTime createdAt;
}

