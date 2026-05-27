package ru.quperino.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import ru.quperino.model.RabbitQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест конфигурации RabbitMQ для модуля Dispatcher.
 * Проверяет, что бины очередей созданы с правильными именами и являются durable.
 */
@SpringBootTest(classes = DispatcherRabbitMQConfig.class)
@ActiveProfiles("test")
class DispatcherRabbitMQConfigTest {
    @Autowired
    private ApplicationContext context;

    @Test
    void textMessageQueueBean_shouldExistWithCorrectNameAndDurable() {
        Queue queue = context.getBean("textMessageQueue", Queue.class);
        assertThat(queue.getName()).isEqualTo(RabbitQueue.TEXT_MESSAGE_UPDATE);
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void answerMessageQueueBean_shouldExistWithCorrectNameAndDurable() {
        Queue queue = context.getBean("answerMessageQueue", Queue.class);
        assertThat(queue.getName()).isEqualTo(RabbitQueue.ANSWER_MESSAGE);
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void callbackQueryQueueBean_shouldExistWithCorrectNameAndDurable() {
        Queue queue = context.getBean("callbackQueryQueue", Queue.class);
        assertThat(queue.getName()).isEqualTo(RabbitQueue.CALLBACK_QUERY_UPDATE);
        assertThat(queue.isDurable()).isTrue();
    }
}
