package ru.quperino.dispatchers;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.quperino.services.impls.DispatcherUpdateProducerServiceImpl;

import static ru.quperino.model.RabbitQueue.CALLBACK_QUERY_UPDATE;
import static ru.quperino.model.RabbitQueue.TEXT_MESSAGE_UPDATE;

/**
 * Диспетчер входящих обновлений от Telegram.
 * <p>
 * Получает объект {@link Update} от TelegramBotController, определяет его тип
 * (текстовое сообщение или callback-запрос) и отправляет в соответствующую очередь RabbitMQ.
 * <p>
 * Сам не обрабатывает логику, только маршрутизирует.
 */
@Component
@Log4j2
public class UpdateDispatcher {
    private final DispatcherUpdateProducerServiceImpl updateProducer;

    @Autowired
    public UpdateDispatcher(DispatcherUpdateProducerServiceImpl updateProducer) {
        this.updateProducer = updateProducer;
    }

    /**
     * Основной метод обработки входящего обновления.
     *
     * @param update объект от Telegram (может быть null)
     * @return всегда {@code null}, так как ответ отправляется асинхронно через RabbitMQ
     */
    public SendMessage processUpdate(Update update) {
        if (update == null) {
            log.error("[DISPATCHER] Получен update, содержащий в себе только null");
            return null;
        }

        // Если есть текст сообщения – обрабатываем как текстовое
        if (update.getMessage() != null && update.getMessage().hasText()) {
            return processTextMessage(update);
        }
        // Если есть callback-запрос (нажатие кнопки) – обрабатываем как callback
        else if (update.hasCallbackQuery()) {
            return processCallbackQuery(update);
        } else {
            log.warn("[DISPATCHER] Получено обновление без текста или callback: {}", update);
            return null;
        }
    }

    /**
     * Обрабатывает callback-запрос (нажатие инлайн-кнопки).
     * Отправляет объект CallbackQuery в очередь CALLBACK_QUERY_UPDATE.
     *
     * @param update обновление, содержащее CallbackQuery
     * @return всегда null
     */
    private SendMessage processCallbackQuery(Update update) {
        updateProducer.produce(CALLBACK_QUERY_UPDATE, update.getCallbackQuery());
        return null;
    }

    /**
     * Обрабатывает текстовое сообщение.
     * Отправляет полный объект Update в очередь TEXT_MESSAGE_UPDATE.
     *
     * @param update обновление, содержащее Message с текстом
     * @return всегда null
     */
    private SendMessage processTextMessage(Update update) {
        updateProducer.produce(TEXT_MESSAGE_UPDATE, update);
        return null;
    }
}
