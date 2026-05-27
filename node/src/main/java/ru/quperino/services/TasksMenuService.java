package ru.quperino.services;

/**
 * Сервис для отправки пользователю списков занятий или задач в зависимости от секции.
 */
public interface TasksMenuService {
    /**
     * Отправляет список занятий для указанной секции (только для METHODOLOGY).
     *
     * @param chatId  ID чата
     * @param section название секции ("METHODOLOGY")
     */
    void sendLessonsBySection(Long chatId, String section);

    /**
     * Отправляет список задач для конкретного занятия.
     *
     * @param chatId       ID чата
     * @param section      секция (только METHODOLOGY)
     * @param lessonNumber номер занятия
     */
    void sendTasksByLesson(Long chatId, String section, Integer lessonNumber);

    /**
     * Отправляет список всех задач секции (используется для ADVANCED).
     *
     * @param chatId  ID чата
     * @param section секция ("ADVANCED")
     */
    void sendTasksBySection(Long chatId, String section);
}
