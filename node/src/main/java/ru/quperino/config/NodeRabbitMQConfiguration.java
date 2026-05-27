package ru.quperino.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static ru.quperino.model.RabbitQueue.SOLUTION_CHECK_QUEUE;

/**
 * Конфигурация RabbitMQ для модуля Node.
 * Объявляет очередь SOLUTION_CHECK_QUEUE, в которую отправляются запросы на проверку SQL.
 */
@Configuration
public class NodeRabbitMQConfiguration {
    @Bean
    public Queue solutionCheckQueue() {
        // durable = true – очередь сохраняется при перезапуске RabbitMQ
        return new Queue(SOLUTION_CHECK_QUEUE, true);
    }
}
