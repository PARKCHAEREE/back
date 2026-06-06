package com.solarwise.capstonebackend.repository;

import com.solarwise.capstonebackend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAsc(Long sessionId); // Long으로 복원
}
