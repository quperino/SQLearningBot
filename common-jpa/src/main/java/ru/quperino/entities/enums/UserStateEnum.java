package ru.quperino.entities.enums;

/**
 * Состояния конечного автомата пользователя.
 * - BASIC_STATE: обычный режим, ожидание команд
 * - WAIT_FOR_EMAIL_STATE: ожидание ввода email при регистрации
 * - WAIT_FOR_TASK_SOLUTION_STATE: ожидание SQL-запроса для обычного решения
 * - WAIT_FOR_TRAINING_SOLUTION_STATE: ожидание SQL-запроса для тренировки (без баллов)
 */
public enum UserStateEnum {
    BASIC_STATE,
    WAIT_FOR_EMAIL_STATE,
    WAIT_FOR_TASK_SOLUTION_STATE,
    WAIT_FOR_TRAINING_SOLUTION_STATE
}
