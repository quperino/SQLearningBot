package ru.quperino.services;

import ru.quperino.entities.Task;

import java.util.List;

/**
 * Сервис для получения данных о задачах (упражнениях).
 */
public interface TaskService {
    /**
     * Находит задачу по её ID.
     *
     * @param id идентификатор задачи
     * @return задача или null, если не найдена
     */
    Task getTaskById(Long id);

    /**
     * Возвращает все задачи указанной секции, отсортированные по ID.
     *
     * @param section название секции ("METHODOLOGY" или "ADVANCED")
     * @return список задач
     */
    List<Task> getTasksBySection(String section);
}
