package ru.quperino.services;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

/**
 * Сервис для обработки callback-запросов (нажатий инлайн-кнопок).
 * Реализует навигацию по секциям, занятиям, задачам, тренировочный режим,
 * сброс решения, пагинацию истории.
 */
public interface CallbackQueryProcessorService {
    /**
     * Основной метод обработки callback-запроса.
     * <p>
     * Анализирует данные кнопки (callbackData) и выполняет соответствующее действие:
     * <ul>
     *   <li>переход к секции / занятию / задаче</li>
     *   <li>возврат назад</li>
     *   <li>сброс прогресса</li>
     *   <li>пагинация истории и т.д.</li>
     * </ul>
     *
     * @param callbackQuery объект CallbackQuery от Telegram
     */
    void processCallback(CallbackQuery callbackQuery);

    /**
     * Отправляет пользователю главное меню выбора секций (Методичка / Повышенный уровень).
     *
     * @param chatId ID чата (строка)
     */
    void sendSectionsMenu(String chatId);
}
