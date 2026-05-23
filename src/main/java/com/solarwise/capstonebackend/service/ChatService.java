package com.solarwise.capstonebackend.service;

import com.solarwise.capstonebackend.dto.chat.ChatMessageRequest;
import com.solarwise.capstonebackend.dto.chat.ChatMessageResponse;
import com.solarwise.capstonebackend.dto.chat.ChatSessionResponse;
import com.solarwise.capstonebackend.entity.ChatMessage;
import com.solarwise.capstonebackend.entity.ChatSession;
import com.solarwise.capstonebackend.entity.PowerPlant;
import com.solarwise.capstonebackend.repository.ChatMessageRepository;
import com.solarwise.capstonebackend.repository.ChatSessionRepository;
import com.solarwise.capstonebackend.repository.PowerPlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PowerPlantRepository powerPlantRepository;
    private final SimulationService simulationService;

    /**
     * 새로운 채팅 세션 생성
     */
    public ChatSessionResponse createSession(Long plantId, String sessionTitle) {
        PowerPlant plant = powerPlantRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("발전소를 찾을 수 없습니다. ID: " + plantId));

        LocalDateTime now = simulationService.getVirtualCurrentTime();

        ChatSession session = ChatSession.builder()
                .powerPlant(plant)
                .sessionTitle(sessionTitle)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ChatSession savedSession = chatSessionRepository.save(session);

        return ChatSessionResponse.builder()
                .sessionId(savedSession.getId())
                .sessionTitle(savedSession.getSessionTitle())
                .plantId(savedSession.getPowerPlant().getId())
                .updatedAt(savedSession.getUpdatedAt())
                .build();
    }

    /**
     * 발전소의 모든 채팅 세션 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getSessionsByPlant(Long plantId) {
        List<ChatSession> sessions = chatSessionRepository.findByPowerPlantIdOrderByUpdatedAtDesc(plantId);

        return sessions.stream()
                .map(session -> ChatSessionResponse.builder()
                        .sessionId(session.getId())
                        .sessionTitle(session.getSessionTitle())
                        .plantId(session.getPowerPlant().getId())
                        .updatedAt(session.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 세션의 이전 메시지 내역 조회 (생성일시 오름차순)
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(sessionId);

        return messages.stream()
                .map(msg -> ChatMessageResponse.builder()
                        .messageId(msg.getId())
                        .senderRole(msg.getSenderRole())
                        .content(msg.getContent())
                        .imageUrl(msg.getImageUrl())
                        .createdAt(msg.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 메시지 전송 (사용자 메시지 저장 + 더미 AI 응답 생성)
     */
    public ChatMessageResponse sendMessage(Long sessionId, ChatMessageRequest request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("세션을 찾을 수 없습니다. ID: " + sessionId));

        LocalDateTime now = simulationService.getVirtualCurrentTime();

        // Step 1: 사용자 메시지 저장
        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(session)
                .senderRole("USER")
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .createdAt(now)
                .build();

        chatMessageRepository.save(userMessage);

        // Step 2: 더미 AI 응답 메시지 생성 및 저장
        LocalDateTime aiResponseTime = now.plusSeconds(1);
        ChatMessage aiMessage = ChatMessage.builder()
                .chatSession(session)
                .senderRole("AI")
                .content("AI가 분석 중입니다.")
                .imageUrl(null)
                .createdAt(aiResponseTime)
                .build();

        ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);

        // Step 3: 세션 updatedAt 업데이트
        session.setUpdatedAt(aiResponseTime);
        chatSessionRepository.save(session);

        // AI 메시지를 응답으로 반환
        return ChatMessageResponse.builder()
                .messageId(savedAiMessage.getId())
                .senderRole(savedAiMessage.getSenderRole())
                .content(savedAiMessage.getContent())
                .imageUrl(savedAiMessage.getImageUrl())
                .createdAt(savedAiMessage.getCreatedAt())
                .build();
    }
}

