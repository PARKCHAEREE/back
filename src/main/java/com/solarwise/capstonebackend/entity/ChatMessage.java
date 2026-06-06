package com.solarwise.capstonebackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id", nullable = false) // columnDefinition 제거
    private ChatSession chatSession;

    @Column(nullable = false)
    private String senderRole; // "USER" or "AI"

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private String imageUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
