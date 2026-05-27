package ru.quperino.services.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тест {@link DispatcherUpdateProducerServiceImpl}.
 * Проверяет, что объекты Update и CallbackQuery корректно сериализуются в JSON
 * и отправляются в RabbitMQ.
 */
@ExtendWith(MockitoExtension.class)
class DispatcherUpdateProducerServiceImplTest {
    private final String testQueue = "test_queue";
    private final String testJson = "{\"key\":\"value\"}";
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private DispatcherUpdateProducerServiceImpl producer;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenReturn(testJson);
    }

    @Test
    void produce_withUpdate_shouldSerializeAndSendToQueue() throws JsonProcessingException {
        Update update = new Update();
        producer.produce(testQueue, update);

        verify(objectMapper).writeValueAsString(update);
        verify(rabbitTemplate).convertAndSend(eq(testQueue), eq(testJson));
    }

    @Test
    void produce_withCallbackQuery_shouldSerializeAndSendToQueue() throws JsonProcessingException {
        CallbackQuery callback = new CallbackQuery();
        producer.produce(testQueue, callback);

        verify(objectMapper).writeValueAsString(callback);
        verify(rabbitTemplate).convertAndSend(eq(testQueue), eq(testJson));
    }
}
