package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.chat.ChatMessageRequest;
import com.solarwise.capstonebackend.dto.chat.ChatMessageResponse;
import com.solarwise.capstonebackend.dto.chat.ChatSessionRequest;
import com.solarwise.capstonebackend.dto.chat.ChatSessionResponse;
import com.solarwise.capstonebackend.entity.ChatMessage;
import com.solarwise.capstonebackend.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Chat", description = "이상 원인 설명 챗 API")
@RestController
@RequestMapping("/api/v1/plants/{plantId}/chat/sessions")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "챗 세션 생성", description = "이상 이벤트를 기반으로 챗 세션을 시작합니다. eventId가 없으면 최근의 중요 이벤트로 자동 설정됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createSession(
            @PathVariable Long plantId,
            @RequestBody(required = false) ChatSessionRequest request) { // 💡 처방 1: required = false
        
        Long eventId = (request != null) ? request.getEventId() : null;
        
        ChatSessionResponse response = chatService.createSessionForEvent(plantId, eventId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "챗 세션이 생성되었습니다."));
    }

    @Operation(summary = "챗 메시지 전송", description = "원인 설명 챗에 질문을 전송하고 AI의 답변을 받습니다.")
    @PostMapping("/{sessionId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @PathVariable Long plantId,
            @PathVariable String sessionId,
            @RequestBody ChatMessageRequest request) {

        Long realSessionId;
        try {
            String[] parts = sessionId.split("_");
            realSessionId = Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 형식의 세션 ID입니다.");
        }

        ChatMessageResponse response = chatService.sendMessage(realSessionId, request.getContent());
        return ResponseEntity.ok(ApiResponse.success(response, "답변 생성 성공"));
    }

    @Operation(summary = "[참고] 해당 발전소의 모든 채팅 세션 조회", description = "특정 발전소의 모든 채팅 세션 목록을 최신순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> getSessions(@PathVariable Long plantId) {
        List<ChatSessionResponse> responses = chatService.getSessionsByPlant(plantId);
        return ResponseEntity.ok(ApiResponse.success(responses, "채팅 세션 목록을 조회했습니다."));
    }

    @Operation(summary = "[참고] 특정 세션의 이전 메시지 내역 조회", description = "특정 세션의 모든 메시지 기록을 시간순으로 조회합니다.")
    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @PathVariable Long plantId,
            @PathVariable String sessionId) {
        
        Long realSessionId;
        try {
            String[] parts = sessionId.split("_");
            realSessionId = Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 형식의 세션 ID입니다.");
        }

        List<ChatMessage> rawMessages = chatService.getMessagesBySessionId(realSessionId);
        List<ChatMessageResponse> responses = rawMessages.stream()
                .map(msg -> ChatMessageResponse.builder()
                        .answer(msg.getContent())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses, "메시지 내역을 조회했습니다."));
    }
}
