package ru.quperino.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Сообщение, полученное от пользователя.
 * Фиксируется вместе с временем обработки.
 */
@Entity
@Table(name = "user_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMessage {
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
