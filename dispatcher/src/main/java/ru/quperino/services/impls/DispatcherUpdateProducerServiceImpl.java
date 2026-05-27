package ru.quperino.services.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.quperino.services.DispatcherUpdateProducerService;

/**
 * Реализация продюсера для отправки обновлений в RabbitMQ.
 * <p>
 * Сериализует объекты Update и CallbackQuery в JSON и отправляет в указанную очередь.
 */
@Service
@Log4j2
public class DispatcherUpdateProducerServiceImpl implements DispatcherUpdateProducerService {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public DispatcherUpdateProducerServiceImpl(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void produce(String rabbitQueue, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            rabbitTemplate.convertAndSend(rabbitQueue, json);
            if (data instanceof Update update) {
                if (update.getMessage() != null) {
                    log.debug("[DISPATCHER] Отправлено текстовое сообщение в JSON: {}", json);
                } else if (update.hasCallbackQuery()) {
                    log.debug("[DISPATCHER] Отправлен callbackQuery в JSON: {}", json);
                }
            } else if (data instanceof CallbackQuery) {
                log.debug("[DISPATCHER] Отправлен CallbackQuery в JSON: {}", json);
            }
        } catch (JsonProcessingException e) {
            log.error("[DISPATCHER] Ошибка сериализации объекта", e);
        }
    }
}
