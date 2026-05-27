package ru.quperino.model;

/**
 * Содержит имена очередей RabbitMQ, используемых для обмена сообщениями
 * между модулями Dispatcher и Node.
 * <p>
 * Все очереди являются durable (сохраняются при перезапуске брокера).
 */
public class RabbitQueue {
    /**
     * Очередь для текстовых сообщений от пользователей.
     * Dispatcher отправляет сюда Update с текстом.
     * Node читает, обрабатывает и отправляет ответ.
     */
    public static final String TEXT_MESSAGE_UPDATE = "text_message_update";

    /**
     * Очередь для ответов от Node к Dispatcher (отправка сообщений пользователю).
     * Содержит обёртку с типом действия (SEND_MESSAGE, DELETE_MESSAGE и т.д.)
     * и соответствующими данными.
     */
    public static final String ANSWER_MESSAGE = "answer_message";

    /**
     * Очередь для запросов на проверку SQL-решений.
     * Node отправляет сюда DTO SolutionCheckRequest,
     * а AI-сервис (или Node-обработчик) потребляет и возвращает результат.
     */
    public static final String SOLUTION_CHECK_QUEUE = "solution_check_queue";

    /**
     * Очередь для команд редактирования сообщений (удаление клавиатуры).
     * Используется, чтобы убрать inline-кнопки после отправки решения.
     * Данные передаются в виде {@link ru.quperino.dto.EditMessageReplyMarkupRequest}.
     */
    public static final String CALLBACK_QUERY_UPDATE = "callback_query_update";
}
