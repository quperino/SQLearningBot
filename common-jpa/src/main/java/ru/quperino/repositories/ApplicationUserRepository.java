package ru.quperino.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.quperino.entities.ApplicationUser;

/**
 * Репозиторий для работы с сущностью {@link ApplicationUser}.
 * Предоставляет базовые CRUD-операции и дополнительные методы поиска.
 */
public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> {
    /**
     * Находит пользователя по его уникальному идентификатору в Telegram.
     * <p>
     * Используется при обработке любого входящего сообщения или callback-запроса:
     * нужно быстро получить или создать запись пользователя.
     *
     * @param telegramUserId ID пользователя, полученный от Telegram API (как строка)
     * @return объект {@link ApplicationUser} или {@code null}, если пользователь не найден
     */
    ApplicationUser findApplicationUserByTelegramUserId(String telegramUserId);
}
