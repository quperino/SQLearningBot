package ru.quperino.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для запроса на удаление inline-клавиатуры у существующего сообщения.
 * Используется в очереди EDIT_MESSAGE_REPLY_MARKUP.
 * <p>
 * Когда пользователь отправляет решение задачи, мы убираем кнопки
 * ("Подсказка", "Назад"), чтобы они не отвлекали.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditMessageReplyMarkupRequest {
    // ID чата (строка, так как Telegram принимает как число, так и строку)
    private String chatId;

    // ID сообщения, у которого нужно изменить клавиатуру
    private Integer messageId;
}
