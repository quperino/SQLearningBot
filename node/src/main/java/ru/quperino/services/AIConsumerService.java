package ru.quperino.services;

/**
 * Консюмер для приёма запросов на проверку решений из очереди SOLUTION_CHECK_QUEUE.
 */
public interface AIConsumerService {
    /**
     * Принимает JSON-запрос на проверку SQL, вызывает AIEvaluationService
     * и отправляет результат пользователю.
     *
     * @param jsonRequest JSON-строка с объектом SolutionCheckRequest
     */
    void consumeSolutionCheck(String jsonRequest);
}
