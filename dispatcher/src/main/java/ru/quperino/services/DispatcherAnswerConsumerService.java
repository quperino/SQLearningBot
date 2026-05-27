package ru.quperino.services;

import org.springframework.amqp.core.Message;

/**
 * Интерфейс консюмера для приёма ответов из очереди ANSWER_MESSAGE.
 * Dispatcher получает команды от Node (отправить сообщение, удалить сообщение, отредактировать клавиатуру и т.д.).
 */
public interface DispatcherAnswerConsumerService {
    /**
     * Обрабатывает входящее сообщение из очереди ANSWER_MESSAGE.
     *
     * @param message сообщение RabbitMQ (содержит JSON-обёртку с типом и данными)
     */
    void consume(Message message);
}
