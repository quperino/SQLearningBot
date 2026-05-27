package ru.quperino.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO для статистики пользователя (команда /stats).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserStatistics {
    // Всего баллов
    private int points;

    // Статистика по занятиям методички
    private List<LessonStats> methodologyLessonStats;

    // Решено задач advanced
    private int advancedTotalSolved;

    // Баллов за advanced
    private int advancedTotalPoints;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LessonStats {
        // Номер занятия
        private Integer lessonNumber;

        // Название занятия
        private String lessonTitle;

        // Количество решённых задач в этом занятии
        private long solvedTasks;

        // Сумма баллов за решённые задачи
        private int earnedPoints;
    }
}
