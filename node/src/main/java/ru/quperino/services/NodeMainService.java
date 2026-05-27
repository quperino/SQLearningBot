package ru.quperino.services;

import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Главный сервис обработки текстовых сообщений от пользователя.
 * <p>
 * Реализует конечный автомат пользователя, обрабатывает команды и переключения состояний:
 * <ul>
 *   <li>BASIC_STATE – ожидание команд</li>
 *   <li>WAIT_FOR_EMAIL_STATE – ожидание ввода email при регистрации</li>
 *   <li>WAIT_FOR_TASK_SOLUTION_STATE – ожидание SQL-запроса для обычного решения</li>
 *   <li>WAIT_FOR_TRAINING_SOLUTION_STATE – ожидание SQL-запроса для тренировки (без начисления баллов)</li>
 * </ul>
 */
public interface NodeMainService {
    /**
     * Основной метод обработки входящего обновления (текстовое сообщение или команда).
     * Вызывается из NodeAnswerConsumerServiceImpl при получении сообщения из очереди TEXT_MESSAGE_UPDATE.
     *
     * @param update объект Update, содержащий сообщение пользователя
     */
    void processTextMassage(Update update);

    /**
     * Останавливает периодическую отправку индикатора "печатает" для указанного чата.
     * Индикатор запускается при отправке решения на проверку и останавливается после получения ответа.
     *
     * @param chatId ID чата (канал с пользователем)
     */
    void stopTypingIndicator(Long chatId);
}
