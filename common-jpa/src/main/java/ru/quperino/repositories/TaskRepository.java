package ru.quperino.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.quperino.entities.Task;

import java.util.List;

/**
 * Репозиторий для работы с задачами (упражнениями по SQL).
 * Содержит методы для навигации по секциям, занятиям и номерам задач.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {
    /**
     * Возвращает все задачи заданной секции, отсортированные по первичному ключу.
     * <p>
     * Используется для секции "ADVANCED", где нет разбивки на занятия.
     *
     * @param section название секции ("METHODOLOGY" или "ADVANCED")
     * @return список задач
     */
    List<Task> findBySectionOrderById(String section);

    /**
     * Возвращает уникальные занятия (номер + название) для указанной секции.
     * <p>
     * Результат – список массивов Object[], где первый элемент – номер занятия,
     * второй – его название. Сортировка по номеру занятия.
     * <p>
     * Используется при построении меню "Выберите занятие" для методички.
     *
     * @param section секция (обязательно "METHODOLOGY")
     * @return список занятий
     */
    @Query("SELECT DISTINCT t.lessonNumber, t.lessonTitle FROM Task t WHERE t.section = :section ORDER BY t.lessonNumber")
    List<Object[]> findDistinctLessonsBySection(@Param("section") String section);

    /**
     * Возвращает все задачи конкретного занятия (например, занятия №1 методички),
     * отсортированные по номеру задачи (task_number).
     * <p>
     * Используется для отображения списка задач после выбора занятия.
     *
     * @param section      секция (только "METHODOLOGY")
     * @param lessonNumber номер занятия (1, 2, ...)
     * @return список задач
     */
    List<Task> findBySectionAndLessonNumberOrderByTaskNumberAsc(String section, Integer lessonNumber);
}
