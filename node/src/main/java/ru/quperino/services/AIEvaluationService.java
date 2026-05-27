package ru.quperino.services;

import ru.quperino.dto.SolutionCheckRequest;
import ru.quperino.dto.SolutionCheckResponse;

/**
 * Сервис для отправки SQL-запроса на проверку внешнему AI API
 * (OpenAI-совместимый) и получения результата.
 */
public interface AIEvaluationService {
    /**
     * Отправляет запрос на проверку решения и возвращает ответ AI.
     *
     * @param request объект с текстом задачи, SQL-запросом пользователя и флагом тренировки
     * @return объект SolutionCheckResponse со статусом (valid/invalid/timeout), сообщением и советами
     */
    SolutionCheckResponse evaluate(SolutionCheckRequest request);
}
