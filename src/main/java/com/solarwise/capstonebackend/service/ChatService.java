package com.solarwise.capstonebackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solarwise.capstonebackend.dto.chat.ChatMessageRequest;
import com.solarwise.capstonebackend.dto.chat.ChatMessageResponse;
import com.solarwise.capstonebackend.dto.chat.ChatSessionResponse;
import com.solarwise.capstonebackend.entity.Anomaly;
import com.solarwise.capstonebackend.entity.ChatMessage;
import com.solarwise.capstonebackend.entity.ChatSession;
import com.solarwise.capstonebackend.exception.ResourceNotFoundException;
import com.solarwise.capstonebackend.repository.AnomalyRepository;
import com.solarwise.capstonebackend.repository.ChatMessageRepository;
import com.solarwise.capstonebackend.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AnomalyRepository anomalyRepository;
    private final SimulationService simulationService;
    private final AiIntegrationService aiIntegrationService;
    private final ObjectMapper objectMapper;

    public ChatSessionResponse createSessionForEvent(Long plantId, Long eventId) {
        Anomaly anomaly;
        if (eventId != null) {
            anomaly = anomalyRepository.findByIdAndPowerPlantId(eventId, plantId)
                    .orElseThrow(() -> new ResourceNotFoundException("해당 발전소에서 이벤트를 찾을 수 없습니다."));
        } else {
            // 💡 처방 1: eventId가 없으면, 가장 최근의 HIGH 등급 이벤트를 자동으로 찾음
            anomaly = anomalyRepository.findByPowerPlantIdAndSeverityOrderByDetectedAtDesc(plantId, "HIGH").stream()
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("참조할 중요 이상 이벤트가 없습니다."));
        }

        String sessionTitle = String.format("이상 감지 #%d: %s", anomaly.getId(), anomaly.getSummary());
        String welcomeMessage = String.format("'%s' 이상 이벤트에 대해 궁금한 점을 질문해주세요.", anomaly.getSummary());

        LocalDateTime now = simulationService.getVirtualCurrentTime();
        ChatSession session = ChatSession.builder()
                .powerPlant(anomaly.getPowerPlant())
                .sessionTitle(sessionTitle)
                .relatedEventId(anomaly.getId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        ChatSession savedSession = chatSessionRepository.save(session);

        ChatMessage welcome = ChatMessage.builder()
                .chatSession(savedSession)
                .senderRole("AI")
                .content(welcomeMessage)
                .createdAt(now.plusSeconds(1))
                .build();
        chatMessageRepository.save(welcome);

        return toSessionResponse(savedSession, welcomeMessage);
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getSessionsByPlant(Long plantId) {
        return chatSessionRepository.findByPowerPlantIdOrderByUpdatedAtDesc(plantId)
                .stream()
                .map(session -> toSessionResponse(session, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesBySessionId(Long sessionId) {
        return chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(sessionId);
    }

    public ChatMessageResponse sendMessage(Long sessionId, String userMessageContent) {
        if (userMessageContent == null || userMessageContent.isBlank()) {
            throw new IllegalArgumentException("메시지 내용이 없습니다.");
        }
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("세션을 찾을 수 없습니다. ID: " + sessionId));
        LocalDateTime now = simulationService.getVirtualCurrentTime();

        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(session)
                .senderRole("USER")
                .content(userMessageContent)
                .createdAt(now)
                .build();
        chatMessageRepository.save(userMessage);

        List<Map<String, String>> history = getMessagesBySessionId(sessionId).stream()
                .map(msg -> Map.of("role", msg.getSenderRole().toLowerCase(), "content", msg.getContent()))
                .collect(Collectors.toList());

        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("history", history);
        aiRequest.put("user_message", userMessageContent);

        try {
            Map aiResponseMap = aiIntegrationService.requestChatTurn(aiRequest).get();
            ChatMessageResponse aiResponse = objectMapper.convertValue(aiResponseMap, ChatMessageResponse.class);
            if (aiResponse.getAnswer() == null) {
                aiResponse.setAnswer("죄송합니다. 답변을 생성할 수 없습니다.");
            }

            LocalDateTime aiResponseTime = now.plusSeconds(1);
            ChatMessage aiMessage = ChatMessage.builder()
                .chatSession(session)
                .senderRole("AI")
                .content(aiResponse.getAnswer())
                .createdAt(aiResponseTime)
                .build();
            chatMessageRepository.save(aiMessage);

            session.setUpdatedAt(aiResponseTime);
            chatSessionRepository.save(session);

            return aiResponse;

        } catch (InterruptedException | ExecutionException e) {
            log.error("AI 챗봇 응답 생성 중 오류 발생", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("AI 챗봇 응답을 받아오는 중 오류가 발생했습니다.");
        }
    }

    private ChatSessionResponse toSessionResponse(ChatSession session, String welcomeMessage) {
        String formattedSessionId = "chat_" + session.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + String.format("%03d", session.getId());
        return ChatSessionResponse.builder()
                .sessionId(formattedSessionId)
                .welcomeMessage(welcomeMessage)
                .build();
    }
}
