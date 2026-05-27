package ru.quperino.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.UserMessage;

import java.util.List;

/**
 * Репозиторий для сообщений, полученных от пользователя.
 * Данные используются для анализа времени обработки запросов и для полного сброса.
 */
public interface UserMessageRepository extends JpaRepository<UserMessage, Long> {
    /**
     * Возвращает все сообщения, отправленные конкретным пользователем.
     * <p>
     * Вызывается при полном сбросе данных пользователя (команда /reset)
     * для удаления всей истории переписки.
     *
     * @param user пользователь
     * @return список сообщений (может быть пустым)
     */
    List<UserMessage> findByUser(ApplicationUser user);
}
