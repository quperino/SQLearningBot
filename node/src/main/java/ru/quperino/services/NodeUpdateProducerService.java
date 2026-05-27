package ru.quperino.services;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import ru.quperino.dto.SolutionCheckRequest;

/**
 * Продюсер для отправки сообщений из модуля Node в RabbitMQ.
 * Отправляет команды для Dispatcher (отправить сообщение, удалить сообщение,
 * отредактировать клавиатуру, отправить документ) и запросы на проверку решений.
 */
public interface NodeUpdateProducerService {
    /**
     * Отправляет текстовое сообщение пользователю (через Dispatcher).
     *
     * @param sendMessage объект SendMessage
     */
    void producerAnswer(SendMessage sendMessage);

    /**
     * Отправляет команду на удаление сообщения.
     *
     * @param deleteMessage объект DeleteMessage
     */
    void produceDeleteMessage(DeleteMessage deleteMessage);

    /**
     * Отправляет запрос на проверку SQL-решения в очередь SOLUTION_CHECK_QUEUE.
     *
     * @param request объект SolutionCheckRequest
     */
    void produceSolutionCheck(SolutionCheckRequest request);

    /**
     * Отправляет пользователю документ (CSV или PDF).
     *
     * @param data     содержимое файла
     * @param fileName имя файла
     * @param chatId   ID чата
     */
    void produceDocument(byte[] data, String fileName, String chatId);

    /**
     * Отправляет команду на редактирование сообщения (удаление клавиатуры).
     *
     * @param chatId    ID чата
     * @param messageId ID сообщения
     */
    void produceEditMessageReplyMarkup(Long chatId, Integer messageId);
}
