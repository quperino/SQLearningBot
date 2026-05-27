package ru.quperino.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Сообщение, отправленное ботом пользователю.
 * Используется для аналитики времени ответа.
 */
@Entity
@Table(name = "bot_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BotMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private ApplicationUser user;
    @Column(columnDefinition = "TEXT")
    private String messageText;
    private LocalDateTime createdAt;
    private Long processingTimeMs;
}
