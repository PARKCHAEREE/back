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

    @JsonProperty("assistant_message") // 💡 최종 수정: AI가 주는 이름
    private String answer; // 프론트가 기대하는 이름

    @JsonProperty("quick_replies") // 💡 최종 수정: AI가 주는 이름
    private List<String> references; // 프론트가 기대하는 이름
}
