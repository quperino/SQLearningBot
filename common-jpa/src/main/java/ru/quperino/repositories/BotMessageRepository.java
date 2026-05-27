package ru.quperino.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.BotMessage;

import java.util.List;

/**
 * Репозиторий для сообщений, отправленных ботом пользователю.
 * Данные используются для аналитики времени отклика бота и для полного сброса данных пользователя.
 */
public interface BotMessageRepository extends JpaRepository<BotMessage, Long> {
    /**
     * Возвращает все сообщения, которые бот отправил конкретному пользователю.
     * <p>
     * Применяется в методе {@code resetUserData} сервиса {@code UserService},
     * чтобы удалить всю историю переписки при сбросе прогресса.
     *
     * @param user пользователь, чьи сообщения нужно найти
     * @return список сообщений (может быть пустым)
     */
    List<BotMessage> findByUser(ApplicationUser user);
}
