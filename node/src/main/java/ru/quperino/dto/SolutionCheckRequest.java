package ru.quperino.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса на проверку SQL-решения.
 * Отправляется из NodeMainServiceImpl в очередь SOLUTION_CHECK_QUEUE.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolutionCheckRequest {
    // ID задачи
    private Long taskId;

    // Текст условия задачи
    private String taskText;

    // SQL-запрос пользователя
    private String userSolution;

    // ID чата для отправки ответа
    private Long chatId;

    // ID пользователя (для поиска в БД)
    private Long userId;

    // ID сообщения с условием (чтобы убрать клавиатуру)
    private Integer taskMessageId;

    // true – тренировочный режим (без баллов)
    private boolean training;
}
