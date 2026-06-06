package com.solarwise.capstonebackend.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    @JsonProperty("assistant_message") // 💡 파이썬 코드와 키 일치
    private String answer;

    @JsonProperty("quick_replies") // 💡 파이썬 코드와 키 일치
    private List<String> references;
}
