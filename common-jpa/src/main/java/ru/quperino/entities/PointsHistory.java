package ru.quperino.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * История начисления/списания баллов.
 * Позволяет отследить, за что и когда пользователь получил или потерял баллы.
 */
@Entity
@Table(name = "points_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointsHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private ApplicationUser user;
    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;
    private int points;
    private LocalDateTime createdAt;
}
