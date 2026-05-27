package ru.quperino.services.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import ru.quperino.dto.EditMessageReplyMarkupRequest;
import ru.quperino.dto.SolutionCheckRequest;
import ru.quperino.services.NodeUpdateProducerService;

import java.util.HashMap;
import java.util.Map;

import static ru.quperino.model.RabbitQueue.ANSWER_MESSAGE;
import static ru.quperino.model.RabbitQueue.SOLUTION_CHECK_QUEUE;

/**
 * Реализация {@link NodeUpdateProducerService}.
 * Отправляет команды в очередь ANSWER_MESSAGE (обёрнутые в JSON с полем "type")
 * и запросы на проверку решений в очередь SOLUTION_CHECK_QUEUE.
 */
@Service
@Log4j2
public class NodeUpdateProducerServiceImpl implements NodeUpdateProducerService {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public NodeUpdateProducerServiceImpl(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Универсальный метод для отправки обёрнутого сообщения в ANSWER_MESSAGE.
     *
     * @param type    тип действия (SEND_MESSAGE, DELETE_MESSAGE, EDIT_MESSAGE_REPLY_MARKUP, SEND_DOCUMENT)
     * @param payload объект данных (SendMessage, DeleteMessage, EditMessageReplyMarkupRequest или Map для документа)
     */
    private void sendWithType(String type, Object payload) {
        try {
            Map<String, Object> wrapper = new HashMap<>();
            wrapper.put("type", type);
            wrapper.put("data", payload);
            String json = objectMapper.writeValueAsString(wrapper);
            log.debug("[NODE] Отправка сообщения типа {} в очередь ANSWER_MESSAGE: {}", type, json);
            rabbitTemplate.convertAndSend(ANSWER_MESSAGE, json);
        } catch (JsonProcessingException e) {
            log.error("[NODE] Ошибка сериализации сообщения типа {}", type, e);
        }
    }

    @Override
    public void producerAnswer(SendMessage sendMessage) {
        sendWithType("SEND_MESSAGE", sendMessage);
    }

    @Override
    public void produceDeleteMessage(DeleteMessage deleteMessage) {
        sendWithType("DELETE_MESSAGE", deleteMessage);
    }

    @Override
    public void produceSolutionCheck(SolutionCheckRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            log.debug("[NODE] Отправка SolutionCheckRequest в очередь SOLUTION_CHECK_QUEUE: {}", json);
            rabbitTemplate.convertAndSend(SOLUTION_CHECK_QUEUE, json);
        } catch (JsonProcessingException e) {
            log.error("[NODE] Ошибка сериализации SolutionCheckRequest", e);
        }
    }

    @Override
    public void produceDocument(byte[] data, String fileName, String chatId) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("type", "SEND_DOCUMENT");
        Map<String, Object> payload = new HashMap<>();
        payload.put("data", data);
        payload.put("fileName", fileName);
        payload.put("chatId", chatId);
        wrapper.put("data", payload);
        try {
            String json = objectMapper.writeValueAsString(wrapper);
            rabbitTemplate.convertAndSend(ANSWER_MESSAGE, json);
            log.debug("[NODE] Отправлен документ {} в чат {}", fileName, chatId);
        } catch (JsonProcessingException e) {
            log.error("[NODE] Ошибка сериализации документа", e);
        }
    }

    @Override
    public void produceEditMessageReplyMarkup(Long chatId, Integer messageId) {
        EditMessageReplyMarkupRequest request = new EditMessageReplyMarkupRequest(chatId.toString(), messageId);
        sendWithType("EDIT_MESSAGE_REPLY_MARKUP", request);
    }
}
