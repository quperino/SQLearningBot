package ru.quperino.services.impls;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import ru.quperino.controllers.TelegramBotController;
import ru.quperino.dto.EditMessageReplyMarkupRequest;
import ru.quperino.services.DispatcherAnswerConsumerService;
import ru.quperino.util.JsonValidator;

import java.util.Base64;
import java.util.Map;

import static ru.quperino.model.RabbitQueue.ANSWER_MESSAGE;

/**
 * Реализация консюмера для очереди ANSWER_MESSAGE.
 * <p>
 * Десериализует JSON, определяет тип действия (SEND_MESSAGE, DELETE_MESSAGE, EDIT_MESSAGE_REPLY_MARKUP, SEND_DOCUMENT)
 * и вызывает соответствующий метод у TelegramBotController.
 */
@Service
@Log4j2
public class DispatcherAnswerConsumerServiceImpl implements DispatcherAnswerConsumerService {
    private final TelegramBotController telegramBotController;
    private final ObjectMapper objectMapper;

    @Autowired
    public DispatcherAnswerConsumerServiceImpl(TelegramBotController telegramBotController, ObjectMapper objectMapper) {
        this.telegramBotController = telegramBotController;
        this.objectMapper = objectMapper;
    }

    /**
     * Слушает очередь ANSWER_MESSAGE.
     * Каждое сообщение – JSON с полями "type" и "data".
     *
     * @param message сообщение из RabbitMQ
     */
    @Override
    @RabbitListener(queues = ANSWER_MESSAGE)
    public void consume(Message message) {
        log.debug("[DISPATCHER] consume: получен JSON: {}", new String(message.getBody()));
        String json = new String(message.getBody());

        // Валидация JSON перед десериализацией
        if (JsonValidator.isInvalidJson(json)) {
            log.warn("[DISPATCHER] Получен невалидный JSON в очереди ANSWER_MESSAGE: {}", json);
            return;
        }

        try {
            // Парсим обёртку
            Map<String, Object> wrapper = objectMapper.readValue(json, new TypeReference<>() {
            });
            String type = (String) wrapper.get("type");
            Object data = wrapper.get("data");

            if (type == null) {
                log.warn("[DISPATCHER] Сообщение без поля type: {}", json);
                return;
            }

            // В зависимости от типа выполняем действие
            switch (type) {
                case "SEND_MESSAGE":
                    SendMessage sendMessage = objectMapper.convertValue(data, SendMessage.class);
                    log.debug("[DISPATCHER] Распаршен SendMessage: chatId={}, text={}", sendMessage.getChatId(), sendMessage.getText());
                    telegramBotController.sendAnswerMessage(sendMessage);
                    break;
                case "DELETE_MESSAGE":
                    DeleteMessage deleteMessage = objectMapper.convertValue(data, DeleteMessage.class);
                    log.debug("[DISPATCHER] Распаршен DeleteMessage: chatId={}, messageId={}", deleteMessage.getChatId(), deleteMessage.getMessageId());
                    telegramBotController.sendDeleteMessage(deleteMessage);
                    break;
                case "EDIT_MESSAGE_REPLY_MARKUP":
                    EditMessageReplyMarkupRequest editRequest = objectMapper.convertValue(data, EditMessageReplyMarkupRequest.class);
                    log.debug("[DISPATCHER] Распаршен EditMessageReplyMarkupRequest: chatId={}, messageId={}", editRequest.getChatId(), editRequest.getMessageId());
                    telegramBotController.sendEditMessageReplyMarkup(editRequest);
                    break;
                case "SEND_DOCUMENT":
                    Map<String, Object> docPayload = (Map<String, Object>) data;
                    String base64Data = (String) docPayload.get("data");
                    byte[] fileData = Base64.getDecoder().decode(base64Data);
                    String fileName = (String) docPayload.get("fileName");
                    String docChatId = (String) docPayload.get("chatId");
                    telegramBotController.sendDocument(fileData, fileName, docChatId);
                    break;
                default:
                    log.warn("[DISPATCHER] Неизвестный тип сообщения: {}", type);
            }
        } catch (Exception e) {
            log.error("[DISPATCHER] Ошибка десериализации: {}", json, e);
        }
    }
}
