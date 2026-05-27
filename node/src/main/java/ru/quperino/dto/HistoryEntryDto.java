package ru.quperino.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistoryEntryDto {
    // Секция (METHODOLOGY / ADVANCED)
    private String section;

    // Название задачи
    private String taskTitle;

    // "✅ Решено", "❌ Неверно", "⏳ Не завершено"
    private String status;

    // Дата завершения (или создания, если не завершена)
    private LocalDateTime date;

    // Количество попыток
    private Integer attempts;
}
