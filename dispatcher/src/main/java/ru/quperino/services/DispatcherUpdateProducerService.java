package ru.quperino.services;

/**
 * Интерфейс продюсера для отправки обновлений из Dispatcher в очереди RabbitMQ.
 */
public interface DispatcherUpdateProducerService {
    /**
     * Сериализует переданный объект в JSON и отправляет в указанную очередь RabbitMQ.
     *
     * @param rabbitQueue название очереди (константы из RabbitQueue)
     * @param data        объект для отправки (Update или CallbackQuery)
     */
    void produce(String rabbitQueue, Object data);
}
