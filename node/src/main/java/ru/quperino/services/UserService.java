package ru.quperino.services;

import org.springframework.data.domain.Page;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.quperino.dto.HistoryEntryDto;
import ru.quperino.dto.UserStatistics;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.Task;
import ru.quperino.entities.enums.UserStateEnum;

/**
 * Сервис для управления пользователями: поиск/создание, смена состояния,
 * регистрация email, начисление/списание баллов, статистика, сброс прогресса.
 */
public interface UserService {
    /**
     * Находит пользователя по Telegram-объекту или создаёт нового, если не существует.
     * Сохраняет основные данные: telegramUserId, username, first/last name.
     *
     * @param telegramUser объект User от Telegram API
     * @return существующий или новый ApplicationUser
     */
    ApplicationUser findOrCreateUser(User telegramUser);

    /**
     * Обновляет состояние пользователя (например, переводит в ожидание email).
     *
     * @param user  пользователь
     * @param state новое состояние
     * @return сохранённый пользователь
     */
    ApplicationUser updateUserState(ApplicationUser user, UserStateEnum state);

    /**
     * Проверяет корректность email по регулярному выражению.
     *
     * @param email строка email
     * @return true, если email соответствует формату
     */
    boolean isEmailValid(String email);

    /**
     * Сохраняет email пользователя и переводит его в базовое состояние (BASIC_STATE).
     *
     * @param user  пользователь
     * @param email подтверждённый email
     * @return сохранённый пользователь
     */
    ApplicationUser registerEmail(ApplicationUser user, String email);

    /**
     * Находит пользователя по его внутреннему ID (из базы).
     *
     * @param userId ID пользователя
     * @return пользователь или null
     */
    ApplicationUser findOrCreateUserById(Long userId);

    /**
     * Очищает сессию пользователя: отменяет все PENDING и PROCESSING решения,
     * сбрасывает состояние в BASIC_STATE.
     *
     * @param user пользователь
     */
    void clearUserSession(ApplicationUser user);

    /**
     * Полностью удаляет все данные пользователя: решения, сообщения, клики,
     * обнуляет email, баллы, тренировочную задачу, состояние.
     *
     * @param user пользователь
     */
    void resetUserData(ApplicationUser user);

    /**
     * Начисляет баллы пользователю (только если points > 0).
     * Сохраняет запись в points_history.
     *
     * @param user   пользователь
     * @param points количество баллов (положительное)
     */
    void addPoints(ApplicationUser user, int points);

    /**
     * Собирает расширенную статистику пользователя: баллы, решённые задачи по занятиям,
     * прогресс по секции ADVANCED.
     *
     * @param user пользователь
     * @return объект UserStatistics
     */
    UserStatistics getUserStatistics(ApplicationUser user);

    /**
     * Устанавливает ID задачи для тренировочного режима.
     *
     * @param user   пользователь
     * @param taskId ID задачи
     */
    void setTrainingTask(ApplicationUser user, Long taskId);

    /**
     * Возвращает ID задачи, выбранной для тренировки, или null.
     *
     * @param user пользователь
     * @return ID задачи или null
     */
    Long getTrainingTask(ApplicationUser user);

    /**
     * Очищает тренировочную задачу (сбрасывает trainingTaskId в null).
     *
     * @param user пользователь
     */
    void clearTrainingTask(ApplicationUser user);

    /**
     * Сбрасывает прогресс по конкретной задаче (переводит COMPLETED в PENDING,
     * обнуляет попытки, правильное решение, вычитает баллы).
     *
     * @param user   пользователь
     * @param taskId ID задачи
     */
    void resetTaskProgress(ApplicationUser user, Long taskId);

    /**
     * Списывает баллы у пользователя (используется при сбросе задачи).
     * Сохраняет запись в points_history с отрицательным значением.
     *
     * @param user   пользователь
     * @param points количество баллов для списания (положительное, но метод вычтет)
     * @param task   задача, за которую были начислены баллы
     */
    void subtractPoints(ApplicationUser user, int points, Task task);

    /**
     * Возвращает страницу истории решений пользователя с пагинацией.
     *
     * @param user пользователь
     * @param page номер страницы (0-based)
     * @param size размер страницы (обычно 5)
     * @return страница с HistoryEntryDto
     */
    Page<HistoryEntryDto> getUserHistory(ApplicationUser user, int page, int size);
}
