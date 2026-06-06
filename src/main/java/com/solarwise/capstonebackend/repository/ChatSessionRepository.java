package com.solarwise.capstonebackend.repository;

import com.solarwise.capstonebackend.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> { // Long으로 복원
    List<ChatSession> findByPowerPlantIdOrderByUpdatedAtDesc(Long plantId);
}
