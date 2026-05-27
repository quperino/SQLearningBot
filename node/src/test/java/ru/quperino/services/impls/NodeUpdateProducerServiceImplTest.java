package ru.quperino.services.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import ru.quperino.dto.SolutionCheckRequest;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Тесты продюсера сообщений из Node в RabbitMQ.
 * Проверяется, что объекты правильно обёртываются в JSON с полем "type"
 * и отправляются в нужные очереди.
 */
@ExtendWith(MockitoExtension.class)
class NodeUpdateProducerServiceImplTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NodeUpdateProducerServiceImpl updateProducer;

    private SendMessage sendMessage;
    private DeleteMessage deleteMessage;
    private SolutionCheckRequest solutionCheckRequest;

    @BeforeEach
    void setUp() {
        sendMessage = new SendMessage();
        sendMessage.setChatId("12345");
        sendMessage.setText("Test message");

        deleteMessage = new DeleteMessage("12345", 100);

        solutionCheckRequest = new SolutionCheckRequest(1L, "task text", "user solution", 12345L, 1L, null, false);
    }

    /**
     * Отправка SendMessage: должна создаться обёртка {type:"SEND_MESSAGE", data:...}.
     */
    @Test
    void producerAnswer_shouldSendWrappedSendMessage() throws JsonProcessingException {
        String expectedJson = "{\"type\":\"SEND_MESSAGE\",\"data\":{}}";
        doReturn(expectedJson).when(objectMapper).writeValueAsString(anyMap());

        updateProducer.producerAnswer(sendMessage);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(objectMapper).writeValueAsString(captor.capture());
        Map<String, Object> wrapper = captor.getValue();
        assertThat(wrapper).containsEntry("type", "SEND_MESSAGE");
        assertThat(wrapper).containsKey("data");
        assertThat(wrapper.get("data")).isEqualTo(sendMessage);

        verify(rabbitTemplate).convertAndSend(anyString(), eq(expectedJson));
    }

    /**
     * Отправка DeleteMessage: обёртка {type:"DELETE_MESSAGE", data:...}.
     */
    @Test
    void produceDeleteMessage_shouldSendWrappedDeleteMessage() throws JsonProcessingException {
        String expectedJson = "{\"type\":\"DELETE_MESSAGE\",\"data\":{}}";
        doReturn(expectedJson).when(objectMapper).writeValueAsString(anyMap());

        updateProducer.produceDeleteMessage(deleteMessage);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(objectMapper).writeValueAsString(captor.capture());
        Map<String, Object> wrapper = captor.getValue();
        assertThat(wrapper).containsEntry("type", "DELETE_MESSAGE");
        assertThat(wrapper).containsKey("data");
        assertThat(wrapper.get("data")).isEqualTo(deleteMessage);

        verify(rabbitTemplate).convertAndSend(anyString(), eq(expectedJson));
    }

    /**
     * Отправка запроса на проверку решения: НЕ обёртывается, отправляется напрямую как JSON.
     */
    @Test
    void produceSolutionCheck_shouldSendDirectlyWithoutWrapper() throws JsonProcessingException {
        String expectedJson = "{\"taskId\":1,\"taskText\":\"task text\",\"userSolution\":\"user solution\",\"chatId\":12345,\"userId\":1,\"training\":false}";
        doReturn(expectedJson).when(objectMapper).writeValueAsString(solutionCheckRequest);

        updateProducer.produceSolutionCheck(solutionCheckRequest);

        verify(objectMapper).writeValueAsString(solutionCheckRequest);
        verify(rabbitTemplate).convertAndSend(anyString(), eq(expectedJson));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), any(Map.class));
    }

    /**
     * Отправка документа: создаётся обёртка {type:"SEND_DOCUMENT", data:{data: base64, fileName, chatId}}.
     */
    @Test
    void produceDocument_shouldSendWrappedDocumentWithBase64Data() throws JsonProcessingException {
        byte[] data = "test,csv".getBytes();
        String fileName = "export.csv";
        String chatId = "12345";
        String base64Data = Base64.getEncoder().encodeToString(data);
        String expectedJson = "{\"type\":\"SEND_DOCUMENT\",\"data\":{\"data\":\"" + base64Data + "\",\"fileName\":\"" + fileName + "\",\"chatId\":\"" + chatId + "\"}}";
        doReturn(expectedJson).when(objectMapper).writeValueAsString(anyMap());

        updateProducer.produceDocument(data, fileName, chatId);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(objectMapper).writeValueAsString(captor.capture());
        Map<String, Object> wrapper = captor.getValue();
        assertThat(wrapper).containsEntry("type", "SEND_DOCUMENT");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) wrapper.get("data");
        assertThat(payload).containsEntry("data", data);
        assertThat(payload).containsEntry("fileName", fileName);
        assertThat(payload).containsEntry("chatId", chatId);

        verify(rabbitTemplate).convertAndSend(anyString(), eq(expectedJson));
    }
}
