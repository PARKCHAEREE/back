package com.solarwise.capstonebackend.dto.chat;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequest {

    private String senderRole; // "USER"

    private String content;

    private String imageUrl;
}

