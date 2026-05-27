package ru.quperino.services.impls;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.UserTaskSolution;
import ru.quperino.entities.enums.SolutionStatus;
import ru.quperino.repositories.UserTaskSolutionRepository;
import ru.quperino.services.ExportService;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Реализация {@link ExportService}.
 * <ul>
 *   <li>Формирует CSV-файл со всеми решениями пользователя.</li>
 *   <li>Добавляет BOM (U+FEFF) для корректного отображения кириллицы в Excel.</li>
 *   <li>Экранирует поля, содержащие точку с запятой, кавычки или перевод строки.</li>
 * </ul>
 */
@Service
@Log4j2
public class ExportServiceImpl implements ExportService {
    private final UserTaskSolutionRepository solutionRepository;

    public ExportServiceImpl(UserTaskSolutionRepository solutionRepository) {
        this.solutionRepository = solutionRepository;
    }

    @Override
    public byte[] generateExportCsv(ApplicationUser user) {
        List<UserTaskSolution> solutions = solutionRepository.findAllByUserOrderByCompletedAtDesc(user);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(0xEF);
        byteArrayOutputStream.write(0xBB);
        byteArrayOutputStream.write(0xBF);
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(byteArrayOutputStream, StandardCharsets.UTF_8))) {
            // Заголовки с добавленным столбцом "Название занятия"
            writer.println("Название занятия;Название задачи;Текст задачи;Статус;Решение пользователя;Дата;Ответ ИИ");
            for (UserTaskSolution sol : solutions) {
                String lessonTitle;
                if ("METHODOLOGY".equals(sol.getTask().getSection())) {
                    lessonTitle = sol.getTask().getLessonTitle() != null ? sol.getTask().getLessonTitle() : "Занятие " + sol.getTask().getLessonNumber();
                } else {
                    lessonTitle = "Повышенный уровень";
                }
                String taskTitle = escapeCsv(sol.getTask().getTitle());
                String taskText = escapeCsv(sol.getTask().getText());
                String status = translateStatus(sol.getStatus());
                String userSolution = "";
                String aiFeedback = "";
                String date = "";
                if (sol.getCompletedAt() != null) {
                    date = sol.getCompletedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                } else if (sol.getCreatedAt() != null) {
                    date = sol.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
                }
                if (sol.getStatus() == SolutionStatus.COMPLETED || sol.getStatus() == SolutionStatus.FAILED) {
                    userSolution = escapeCsv(sol.getLastCorrectSolution() != null ? sol.getLastCorrectSolution() : "");
                    aiFeedback = escapeCsv(sol.getAiFeedback() != null ? sol.getAiFeedback() : "");
                }
                writer.printf("%s;%s;%s;%s;%s;%s;%s%n",
                        lessonTitle, taskTitle, taskText, status, userSolution, date, aiFeedback);
            }
        } catch (Exception e) {
            log.error("[NODE] Ошибка генерации CSV", e);
            return new byte[0];
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Преобразует статус решения в человеко-читаемую строку для CSV.
     */
    private String translateStatus(SolutionStatus status) {
        return switch (status) {
            case COMPLETED -> "Решено верно";
            case FAILED -> "Решено неверно";
            case PENDING -> "Ожидает решения";
            case PROCESSING -> "Проверяется";
            case CANCELLED -> "Отменено";
            case TIMEOUT -> "Таймаут";
        };
    }

    /**
     * Экранирует поле CSV: если содержит ';', '"' или '\n', заключает в кавычки и дублирует внутренние кавычки.
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
