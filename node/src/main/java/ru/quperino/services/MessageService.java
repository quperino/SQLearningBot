package ru.quperino.services;

import org.telegram.telegrambots.meta.api.objects.Message;
import ru.quperino.entities.ApplicationUser;

/**
 * Сервис для сохранения входящих и исходящих сообщений в БД.
 * Используется для аналитики и для полного сброса данных пользователя.
 */
public interface MessageService {
    /**
     * Сохраняет сообщение, полученное от пользователя.
     *
     * @param user             пользователь
     * @param message          объект Telegram-сообщения
     * @param processingTimeMs время обработки (будет обновлено позже, но можно сохранить 0)
     */
    void saveUserMessage(ApplicationUser user, Message message, Long processingTimeMs);

    /**
     * Сохраняет сообщение, отправленное ботом пользователю.
     *
     * @param user            пользователь
     * @param text            текст сообщения
     * @param processingTimeMs время формирования ответа
     */
    void saveBotMessage(ApplicationUser user, String text, Long processingTimeMs);
}
