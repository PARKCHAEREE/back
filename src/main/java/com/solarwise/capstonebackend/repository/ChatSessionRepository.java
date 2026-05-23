package com.solarwise.capstonebackend.repository;

import com.solarwise.capstonebackend.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    // 발전소 ID로 세션 목록을 최신순(updatedAt 내림차순)으로 조회
    List<ChatSession> findByPowerPlantIdOrderByUpdatedAtDesc(Long powerPlantId);
}

