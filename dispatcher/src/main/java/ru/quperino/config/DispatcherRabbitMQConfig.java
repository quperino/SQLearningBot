package ru.quperino.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static ru.quperino.model.RabbitQueue.*;

/**
 * Конфигурация очередей RabbitMQ для модуля Dispatcher.
 * <p>
 * Создаёт durable-очереди, которые будут использоваться для обмена сообщениями с модулем Node.
 * Названия очередей берутся из констант RabbitQueue.
 */
@Configuration
public class DispatcherRabbitMQConfig {
    /**
     * Очередь для текстовых сообщений от пользователей.
     */
    @Bean
    public Queue textMessageQueue() {
        return new Queue(TEXT_MESSAGE_UPDATE, true);
    }

    /**
     * Очередь для ответов от Node к Dispatcher (отправка сообщений пользователю).
     */
    @Bean
    public Queue answerMessageQueue() {
        return new Queue(ANSWER_MESSAGE, true);
    }

    /**
     * Очередь для callback-запросов (нажатий кнопок).
     */
    @Bean
    public Queue callbackQueryQueue() {
        return new Queue(CALLBACK_QUERY_UPDATE, true);
    }
}
