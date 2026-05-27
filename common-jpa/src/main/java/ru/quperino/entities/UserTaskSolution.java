package ru.quperino.entities;

import jakarta.persistence.*;
import lombok.*;
import ru.quperino.entities.enums.SolutionStatus;

import java.time.LocalDateTime;

/**
 * Сущность, фиксирующая попытку решения задачи пользователем.
 * Хранит статус проверки, feedback от AI, количество попыток, использовалась ли подсказка.
 */
@Entity
@Table(name = "user_task_solutions", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "task_id"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTaskSolution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private ApplicationUser user;
    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    // Статус: PENDING, PROCESSING, COMPLETED, CANCELLED, FAILED, TIMEOUT
    @Enumerated(EnumType.STRING)
    private SolutionStatus status;

    // Текст обратной связи от AI (может содержать разбор ошибок)
    @Column(columnDefinition = "TEXT")
    private String aiFeedback;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    // Последнее правильное решение (сохраняется при успехе)
    @Column(columnDefinition = "TEXT")
    private String lastCorrectSolution;

    // Количество попыток
    @Builder.Default
    private Integer attempts = 0;

    // Флаг, использовал ли пользователь подсказку для этой попытки
    @Builder.Default
    private Boolean hintUsed = false;

    // ID сообщения в Telegram, в котором была отправлена задача (чтобы убрать клавиатуру)
    @Builder.Default
    private Integer taskMessageId = null;
}
