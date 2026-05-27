package ru.quperino.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.quperino.dispatchers.UpdateDispatcher;
import ru.quperino.dto.EditMessageReplyMarkupRequest;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;

/**
 * Контроллер Telegram-бота, наследник TelegramLongPollingBot.
 * <p>
 * Отвечает за:
 * <ul>
 *   <li>Приём обновлений от Telegram (метод onUpdateReceived)</li>
 *   <li>Отправку сообщений, документов, удаление сообщений, редактирование клавиатур</li>
 *   <li>Ответы на callback-запросы (AnswerCallbackQuery)</li>
 * </ul>
 * Сам не обрабатывает логику, а делегирует UpdateDispatcher.
 */
@Component
@Log4j2
public class TelegramBotController extends TelegramLongPollingBot {
    private final String botUsername;
    private final UpdateDispatcher updateDispatcher;

    @Autowired
    public TelegramBotController(@Value("${bot.token}") String botToken,
                                 @Value("${bot.username}") String botUsername,
                                 UpdateDispatcher updateDispatcher) {
        super(botToken);
        this.botUsername = botUsername;
        this.updateDispatcher = updateDispatcher;
    }

    @Override
    public String getBotUsername() {
        return this.botUsername;
    }

    /**
     * Telegram вызывает этот метод при каждом новом обновлении (сообщение, callback и т.д.).
     * Передаёт управление диспетчеру.
     */
    @Override
    public void onUpdateReceived(Update update) {
        SendMessage response = updateDispatcher.processUpdate(update);
        if (response != null) {
            sendAnswerMessage(response);
        }
    }

    // ------------------------------------------------------------------------
    // Методы для отправки различных типов ответов
    // ------------------------------------------------------------------------

    /**
     * Отправляет текстовое сообщение пользователю.
     *
     * @param message объект SendMessage с заполненными chatId и text
     */
    public void sendAnswerMessage(SendMessage message) {
        if (message != null) {
            try {
                log.debug("[DISPATCHER] Отправка SendMessage: chatId={}, text={}", message.getChatId(), message.getText());
                execute(message);
            } catch (TelegramApiException e) {
                log.error("[DISPATCHER] Ошибка отправки сообщения: {}", message, e);
            }
        }
    }

    /**
     * Удаляет сообщение из чата.
     *
     * @param deleteMessage объект с chatId и messageId
     */
    public void sendDeleteMessage(DeleteMessage deleteMessage) {
        if (deleteMessage != null) {
            try {
                log.debug("[DISPATCHER] Удаление сообщения: chatId={}, messageId={}", deleteMessage.getChatId(), deleteMessage.getMessageId());
                execute(deleteMessage);
            } catch (TelegramApiException e) {
                log.error("[DISPATCHER] Ошибка удаления: {}", deleteMessage, e);
            }
        }
    }

    /**
     * Редактирует inline-клавиатуру сообщения (обычно чтобы убрать кнопки).
     *
     * @param request содержит chatId и messageId
     */
    public void sendEditMessageReplyMarkup(EditMessageReplyMarkupRequest request) {
        if (request != null) {
            try {
                EditMessageReplyMarkup edit = new EditMessageReplyMarkup();
                edit.setChatId(request.getChatId());
                edit.setMessageId(request.getMessageId());
                // Вместо null передаём пустую inline-клавиатуру
                edit.setReplyMarkup(new InlineKeyboardMarkup(new ArrayList<>()));
                log.debug("[DISPATCHER] Редактирование сообщения (удаление клавиатуры): chatId={}, messageId={}", request.getChatId(), request.getMessageId());
                execute(edit);
            } catch (TelegramApiException e) {
                log.error("[DISPATCHER] Ошибка редактирования: {}", request, e);
            }
        }
    }

    /**
     * Отвечает на callback-запрос, чтобы Telegram не крутил индикатор загрузки.
     * При необходимости показывает всплывающее уведомление.
     *
     * @param callbackId ID callback-запроса
     * @param text       текст уведомления (может быть null)
     */
    public void sendAnswerCallbackQuery(String callbackId, String text) {
        if (callbackId == null) return;
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackId);
        if (text != null) {
            answer.setText(text);
        }
        answer.setCacheTime(0); // не кэшировать
        try {
            execute(answer);
            log.debug("[DISPATCHER] Отправлен AnswerCallbackQuery для id={}", callbackId);
        } catch (TelegramApiException e) {
            log.error("[DISPATCHER] Ошибка ответа на callback id={}", callbackId, e);
        }
    }

    /**
     * Отправляет сообщение и возвращает его ID (нужно для последующего редактирования).
     *
     * @param message сообщение
     * @return messageId отправленного сообщения или null при ошибке
     */
    public Integer sendAnswerMessageAndGetId(SendMessage message) {
        if (message == null) return null;
        try {
            log.debug("[DISPATCHER] Отправка SendMessage (с получением ID): chatId={}, text={}", message.getChatId(), message.getText());
            var sentMessage = execute(message);
            log.debug("[DISPATCHER] Сообщение отправлено, messageId={}", sentMessage.getMessageId());
            return sentMessage.getMessageId();
        } catch (TelegramApiException e) {
            log.error("[DISPATCHER] Ошибка отправки сообщения: {}", message, e);
            return null;
        }
    }

    /**
     * Отправляет действие "печатает" (typing), чтобы показать пользователю, что бот обрабатывает запрос.
     *
     * @param chatId ID чата
     */
    public void sendChatAction(String chatId) {
        if (chatId == null) return;
        SendChatAction sendAction = new SendChatAction();
        sendAction.setChatId(chatId);
        sendAction.setAction(ActionType.TYPING);
        try {
            execute(sendAction);
            log.debug("[DISPATCHER] Отправлен ChatAction в чат {}", chatId);
        } catch (TelegramApiException e) {
            log.error("[DISPATCHER] Ошибка отправки ChatAction в чат {}", chatId, e);
        }
    }

    /**
     * Отправляет пользователю документ (например, CSV-файл или PDF).
     *
     * @param data     содержимое файла в виде байтового массива
     * @param fileName имя файла
     * @param chatId   ID чата
     */
    public void sendDocument(byte[] data, String fileName, String chatId) {
        try {
            SendDocument sendDocument = new SendDocument();
            sendDocument.setChatId(chatId);
            sendDocument.setDocument(new InputFile(new ByteArrayInputStream(data), fileName));
            execute(sendDocument);
            log.debug("[DISPATCHER] Документ {} отправлен в чат {}", fileName, chatId);
        } catch (TelegramApiException e) {
            log.error("[DISPATCHER] Ошибка отправки документа", e);
        }
    }
}
