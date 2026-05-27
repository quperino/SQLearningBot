package ru.quperino.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.ButtonClick;

import java.util.List;

/**
 * Репозиторий для хранения фактов нажатий на инлайн-кнопки (callback-запросов).
 * Используется для сбора аналитики: какие кнопки и как часто нажимают пользователи.
 */
public interface ButtonClickRepository extends JpaRepository<ButtonClick, Long> {
    /**
     * Возвращает все клики, совершённые указанным пользователем.
     * <p>
     * Необходимо при полном сбросе данных пользователя (команда /reset).
     *
     * @param user пользователь
     * @return список кликов (порядок не определён)
     */
    List<ButtonClick> findByUser(ApplicationUser user);
}
