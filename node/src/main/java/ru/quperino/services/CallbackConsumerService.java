package ru.quperino.services;

/**
 * Консюмер для приёма callback-запросов (нажатий кнопок) от Dispatcher.
 */
public interface CallbackConsumerService {
    /**
     * Обрабатывает JSON-строку, представляющую CallbackQuery.
     * Десериализует и передаёт в CallbackQueryProcessorService.
     *
     * @param jsonCallback JSON-строка с объектом CallbackQuery
     */
    void consumeCallback(String jsonCallback);
}
