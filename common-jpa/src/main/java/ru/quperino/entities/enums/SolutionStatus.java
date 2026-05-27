package ru.quperino.entities.enums;

/**
 * Статусы процесса проверки решения.
 * - PENDING: задача выбрана, ожидается ввод SQL
 * - PROCESSING: SQL отправлен на проверку, ждём ответ AI
 * - COMPLETED: решение верное, баллы начислены
 * - CANCELLED: пользователь отменил или произошёл сброс
 * - FAILED: решение неверное (без возможности повторной отправки?)
 * - TIMEOUT: проверка заняла слишком долго
 */
public enum SolutionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    CANCELLED,
    FAILED,
    TIMEOUT
}
