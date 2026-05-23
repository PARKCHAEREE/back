package com.solarwise.capstonebackend.controller;

import com.solarwise.capstonebackend.dto.ApiResponse;
import com.solarwise.capstonebackend.dto.chat.ChatMessageRequest;
import com.solarwise.capstonebackend.dto.chat.ChatMessageResponse;
import com.solarwise.capstonebackend.dto.chat.ChatSessionResponse;
import com.solarwise.capstonebackend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plants")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * POST /api/v1/plants/{plantId}/chat/sessions
     * 새로운 채팅 세션 생성
     */
    @PostMapping("/{plantId}/chat/sessions")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createSession(
            @PathVariable Long plantId,
            @RequestParam String sessionTitle) {

        ChatSessionResponse response = chatService.createSession(plantId, sessionTitle);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ChatSessionResponse>builder()
                        .success(true)
                        .data(response)
                        .message("채팅 세션이 생성되었습니다.")
                        .build());
    }

    /**
     * GET /api/v1/plants/{plantId}/chat/sessions
     * 해당 발전소의 모든 채팅 세션 조회 (최신순)
     */
    @GetMapping("/{plantId}/chat/sessions")
    public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> getSessions(
            @PathVariable Long plantId) {

        List<ChatSessionResponse> responses = chatService.getSessionsByPlant(plantId);

        return ResponseEntity.ok()
                .body(ApiResponse.<List<ChatSessionResponse>>builder()
                        .success(true)
                        .data(responses)
                        .message("채팅 세션 목록을 조회했습니다.")
                        .build());
    }

    /**
     * GET /api/v1/plants/{plantId}/chat/sessions/{sessionId}/messages
     * 특정 세션의 이전 메시지 내역 조회 (생성일시 오름차순)
     */
    @GetMapping("/{plantId}/chat/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
            @PathVariable Long plantId,
            @PathVariable Long sessionId) {

        List<ChatMessageResponse> responses = chatService.getMessages(sessionId);

        return ResponseEntity.ok()
                .body(ApiResponse.<List<ChatMessageResponse>>builder()
                        .success(true)
                        .data(responses)
                        .message("메시지 내역을 조회했습니다.")
                        .build());
    }

    /**
     * POST /api/v1/plants/{plantId}/chat/sessions/{sessionId}/messages
     * 메시지 전송 (사용자 메시지 저장 + 더미 AI 응답)
     */
    @PostMapping("/{plantId}/chat/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @PathVariable Long plantId,
            @PathVariable Long sessionId,
            @RequestBody ChatMessageRequest request) {

        ChatMessageResponse response = chatService.sendMessage(sessionId, request);

        return ResponseEntity.ok()
                .body(ApiResponse.<ChatMessageResponse>builder()
                        .success(true)
                        .data(response)
                        .message("메시지가 전송되었습니다.")
                        .build());
    }
}

