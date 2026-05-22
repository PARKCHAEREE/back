package com.solarwise.capstonebackend.repository;

import com.solarwise.capstonebackend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 세션 ID로 소속된 메시지 목록을 생성일시 오름차순으로 조회
    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAsc(Long chatSessionId);
}

