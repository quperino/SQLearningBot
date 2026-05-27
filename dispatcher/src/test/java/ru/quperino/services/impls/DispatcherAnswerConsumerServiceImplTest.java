package ru.quperino.services.impls;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import ru.quperino.controllers.TelegramBotController;
import ru.quperino.dto.EditMessageReplyMarkupRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Тест консюмера {@link DispatcherAnswerConsumerServiceImpl}, который обрабатывает
 * сообщения из очереди ANSWER_MESSAGE и вызывает соответствующие методы TelegramBotController.
 */
@ExtendWith(MockitoExtension.class)
class DispatcherAnswerConsumerServiceImplTest {
    @Mock
    private TelegramBotController botController;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DispatcherAnswerConsumerServiceImpl consumer;

    private Message amqpMessage;

    @BeforeEach
    void setUp() {
        amqpMessage = mock(Message.class);
    }

    @Test
    void consume_withInvalidJson_shouldLogAndReturn() throws Exception {
        String invalidJson = "not a json";
        when(amqpMessage.getBody()).thenReturn(invalidJson.getBytes(StandardCharsets.UTF_8));

        consumer.consume(amqpMessage);

        verify(botController, never()).sendAnswerMessage(any());
        verify(objectMapper, never()).readValue(anyString(), any(TypeReference.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consume_withSendMessageType_shouldCallSendAnswerMessage() throws Exception {
        String json = "{\"type\":\"SEND_MESSAGE\",\"data\":{\"chatId\":\"123\",\"text\":\"Hello\"}}";
        when(amqpMessage.getBody()).thenReturn(json.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> wrapper = Map.of("type", "SEND_MESSAGE", "data", Map.of("chatId", "123", "text", "Hello"));
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId("123");
        sendMessage.setText("Hello");

        when(objectMapper.readValue(eq(json), any(TypeReference.class))).thenReturn(wrapper);
        when(objectMapper.convertValue(wrapper.get("data"), SendMessage.class)).thenReturn(sendMessage);

        consumer.consume(amqpMessage);

        verify(botController).sendAnswerMessage(sendMessage);
    }

    @Test
    @SuppressWarnings("unchecked")
    void consume_withDeleteMessageType_shouldCallSendDeleteMessage() throws Exception {
        String json = "{\"type\":\"DELETE_MESSAGE\",\"data\":{\"chatId\":\"123\",\"messageId\":42}}";
        when(amqpMessage.getBody()).thenReturn(json.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> wrapper = Map.of("type", "DELETE_MESSAGE", "data", Map.of("chatId", "123", "messageId", 42));
        DeleteMessage deleteMessage = new DeleteMessage("123", 42);

        when(objectMapper.readValue(eq(json), any(TypeReference.class))).thenReturn(wrapper);
        when(objectMapper.convertValue(wrapper.get("data"), DeleteMessage.class)).thenReturn(deleteMessage);

        consumer.consume(amqpMessage);

        verify(botController).sendDeleteMessage(deleteMessage);
    }

    @Test
    @SuppressWarnings("unchecked")
    void consume_withEditMessageReplyMarkupType_shouldCallSendEditMessageReplyMarkup() throws Exception {
        String json = "{\"type\":\"EDIT_MESSAGE_REPLY_MARKUP\",\"data\":{\"chatId\":\"123\",\"messageId\":42}}";
        when(amqpMessage.getBody()).thenReturn(json.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> wrapper = Map.of("type", "EDIT_MESSAGE_REPLY_MARKUP", "data", Map.of("chatId", "123", "messageId", 42));
        EditMessageReplyMarkupRequest request = new EditMessageReplyMarkupRequest("123", 42);

        when(objectMapper.readValue(eq(json), any(TypeReference.class))).thenReturn(wrapper);
        when(objectMapper.convertValue(wrapper.get("data"), EditMessageReplyMarkupRequest.class)).thenReturn(request);

        consumer.consume(amqpMessage);

        verify(botController).sendEditMessageReplyMarkup(request);
    }

    @Test
    @SuppressWarnings("unchecked")
    void consume_withSendDocumentType_shouldCallSendDocument() throws Exception {
        byte[] fileData = "test,data".getBytes(StandardCharsets.UTF_8);
        String base64Data = Base64.getEncoder().encodeToString(fileData);
        String json = String.format("{\"type\":\"SEND_DOCUMENT\",\"data\":{\"data\":\"%s\",\"fileName\":\"export.csv\",\"chatId\":\"123\"}}", base64Data);
        when(amqpMessage.getBody()).thenReturn(json.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> wrapper = Map.of("type", "SEND_DOCUMENT", "data", Map.of("data", base64Data, "fileName", "export.csv", "chatId", "123"));

        when(objectMapper.readValue(eq(json), any(TypeReference.class))).thenReturn(wrapper);

        consumer.consume(amqpMessage);

        ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(botController).sendDocument(dataCaptor.capture(), eq("export.csv"), eq("123"));
        assertThat(dataCaptor.getValue()).isEqualTo(fileData);
    }

    @Test
    @SuppressWarnings("unchecked")
    void consume_withUnknownType_shouldLogWarning() throws Exception {
        String json = "{\"type\":\"UNKNOWN\",\"data\":{}}";
        when(amqpMessage.getBody()).thenReturn(json.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> wrapper = Map.of("type", "UNKNOWN", "data", Map.of());

        when(objectMapper.readValue(eq(json), any(TypeReference.class))).thenReturn(wrapper);

        consumer.consume(amqpMessage);

        verify(botController, never()).sendAnswerMessage(any());
        verify(botController, never()).sendDeleteMessage(any());
        verify(botController, never()).sendEditMessageReplyMarkup(any());
        verify(botController, never()).sendDocument(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void consume_withNullType_shouldLogWarning() throws Exception {
        String json = "{\"data\":{}}";
        when(amqpMessage.getBody()).thenReturn(json.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> wrapper = Map.of("data", Map.of());

        when(objectMapper.readValue(eq(json), any(TypeReference.class))).thenReturn(wrapper);

        consumer.consume(amqpMessage);

        verifyNoInteractions(botController);
    }
}
