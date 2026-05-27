package ru.quperino.services;

import ru.quperino.entities.ApplicationUser;

/**
 * Сервис для отправки пользователю истории решений с пагинацией.
 */
public interface HistoryService {
    /**
     * Отправляет указанную страницу истории решений в чат.
     *
     * @param user   пользователь
     * @param chatId ID чата
     * @param page   номер страницы (0-based)
     */
    void sendHistoryPage(ApplicationUser user, Long chatId, int page);
}
