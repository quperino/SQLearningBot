package ru.quperino.services;

/**
 * Консюмер для приёма текстовых сообщений от Dispatcher.
 * Реализует метод, аннотированный @RabbitListener(queues = TEXT_MESSAGE_UPDATE).
 */
public interface NodeAnswerConsumerService {
    /**
     * Обрабатывает JSON-строку, представляющую Update от Telegram.
     * Десериализует и передаёт в NodeMainService.
     *
     * @param jsonUpdate JSON-строка с объектом Update
     */
    void consumeTextMessagesUpdates(String jsonUpdate);
}
