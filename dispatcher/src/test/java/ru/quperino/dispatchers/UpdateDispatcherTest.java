package ru.quperino.dispatchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.quperino.services.impls.DispatcherUpdateProducerServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Тест диспетчера {@link UpdateDispatcher}.
 * Проверяет маршрутизацию текстовых сообщений и callback-запросов в соответствующие очереди RabbitMQ.
 */
@ExtendWith(MockitoExtension.class)
class UpdateDispatcherTest {

    @Mock
    private DispatcherUpdateProducerServiceImpl updateProducer;

    @InjectMocks
    private UpdateDispatcher updateDispatcher;

    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        update = new Update();
        message = new Message();
        update.setMessage(message);
    }

    @Test
    void processUpdate_withNullUpdate_shouldLogAndReturnNull() {
        SendMessage result = updateDispatcher.processUpdate(null);
        assertThat(result).isNull();
        verifyNoInteractions(updateProducer);
    }

    @Test
    void processUpdate_withTextMessage_shouldProduceToTextQueueAndReturnNull() {
        // given
        String text = "Hello";
        message.setText(text);

        // when
        SendMessage result = updateDispatcher.processUpdate(update);

        // then
        assertThat(result).isNull();
        verify(updateProducer).produce(eq("text_message_update"), eq(update));
    }

    @Test
    void processUpdate_withCallbackQuery_shouldProduceToCallbackQueueAndReturnNull() {
        // given
        update.setMessage(null);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        update.setCallbackQuery(callbackQuery);

        // when
        SendMessage result = updateDispatcher.processUpdate(update);

        // then
        assertThat(result).isNull();
        verify(updateProducer).produce(eq("callback_query_update"), eq(callbackQuery));
    }

    @Test
    void processUpdate_withNonTextNonCallback_shouldLogWarningAndReturnNull() {
        // given: update has message but no text (e.g., photo)
        message.setText(null);
        update.setMessage(message);
        update.setCallbackQuery(null);

        // when
        SendMessage result = updateDispatcher.processUpdate(update);

        // then
        assertThat(result).isNull();
        verifyNoInteractions(updateProducer);
    }

    @Test
    void processUpdate_withMessageNull_shouldLogWarningAndReturnNull() {
        // given
        update.setMessage(null);
        update.setCallbackQuery(null);

        // when
        SendMessage result = updateDispatcher.processUpdate(update);

        // then
        assertThat(result).isNull();
        verifyNoInteractions(updateProducer);
    }
}
