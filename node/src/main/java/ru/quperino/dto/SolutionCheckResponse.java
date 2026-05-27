package ru.quperino.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для ответа AI о проверке SQL-решения.
 * JSON-ответ парсится в этот объект.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolutionCheckResponse {
    // "valid", "invalid" или "timeout"
    private String status;

    // Текст обратной связи (почему верно/неверно)
    private String message;

    // Дополнительные советы
    private String suggestions;
}
