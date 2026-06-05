package com.solarwise.capstonebackend.service;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    public ChatSessionResponse createSessionForEvent(Long plantId, Long eventId) {
        Anomaly anomaly = anomalyRepository.findByIdAndPowerPlantId(eventId, plantId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 발전소에서 이벤트를 찾을 수 없습니다."));

        String sessionTitle = String.format("이상 감지 #%d: %s", eventId, anomaly.getSummary());
        String welcomeMessage = String.format("'%s' 이상 이벤트에 대해 궁금한 점을 질문해주세요.", anomaly.getSummary());

        LocalDateTime now = simulationService.getVirtualCurrentTime();
        ChatSession session = ChatSession.builder()
                .id(String.format("chat_%s", UUID.randomUUID().toString().substring(0, 8)))
                .powerPlant(anomaly.getPowerPlant())
                .sessionTitle(sessionTitle)
                .relatedEventId(eventId)
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
    public List<ChatMessage> getMessagesBySessionId(String sessionId) {
        return chatMessageRepository.findByChatSessionIdOrderByCreatedAtAsc(sessionId);
    }

    public ChatMessageResponse sendMessage(String sessionId, ChatMessageRequest request) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("세션을 찾을 수 없습니다. ID: " + sessionId));
        LocalDateTime now = simulationService.getVirtualCurrentTime();

        ChatMessage userMessage = ChatMessage.builder()
                .chatSession(session)
                .senderRole("USER")
                .content(request.getContent())
                .createdAt(now)
                .build();
        chatMessageRepository.save(userMessage);

        // AI에게 보낼 대화 기록 생성 (DB에서 직접 엔티티 조회)
        List<Map<String, String>> history = getMessagesBySessionId(sessionId).stream()
                .map(msg -> Map.of("role", msg.getSenderRole().toLowerCase(), "content", msg.getContent()))
                .collect(Collectors.toList());

        Map<String, Object> aiRequest = new HashMap<>();
        aiRequest.put("history", history);
        aiRequest.put("user_message", request.getContent());

        try {
            Map aiResponse = aiIntegrationService.requestChatTurn(aiRequest).get();
            String answer = (String) aiResponse.getOrDefault("assistant_message", "죄송합니다. 답변을 생성할 수 없습니다.");
            List<String> references = (List<String>) aiResponse.get("references");

            LocalDateTime aiResponseTime = now.plusSeconds(1);
            ChatMessage aiMessage = ChatMessage.builder()
                .chatSession(session)
                .senderRole("AI")
                .content(answer)
                .createdAt(aiResponseTime)
                .build();
            chatMessageRepository.save(aiMessage);

            session.setUpdatedAt(aiResponseTime);
            chatSessionRepository.save(session);

            return toMessageResponse(aiMessage, references);

        } catch (InterruptedException | ExecutionException e) {
            log.error("AI 챗봇 응답 생성 중 오류 발생", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("AI 챗봇 응답을 받아오는 중 오류가 발생했습니다.");
        }
    }

    private ChatSessionResponse toSessionResponse(ChatSession session, String welcomeMessage) {
        return ChatSessionResponse.builder()
                .sessionId(session.getId())
                .welcomeMessage(welcomeMessage)
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message, List<String> references) {
        return ChatMessageResponse.builder()
                .answer(message.getContent())
                .references(references)
                .build();
    }
}
