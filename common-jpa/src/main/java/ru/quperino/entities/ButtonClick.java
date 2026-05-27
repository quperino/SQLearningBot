package ru.quperino.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Нажатие на инлайн-кнопку (callback). Хранится для аналитики действий пользователя.
 */
@Entity
@Table(name = "button_clicks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ButtonClick {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private ApplicationUser user;
    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;
    private String buttonName;
    private LocalDateTime createdAt;
}
