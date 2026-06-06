package com.solarwise.capstonebackend.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatMessageRequest {
    // 💡 챗봇 500 에러 해결: 프론트가 보내는 키는 'message'가 아닌 'content'일 수 있음
    // 명세서(8-2)에는 'message'로 되어있지만, 실제 에러 로그는 content가 null이라고 함.
    // 둘 다 받을 수 있도록 @JsonProperty 사용
    @JsonProperty("content")
    private String content;
}
