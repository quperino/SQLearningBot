package ru.quperino.services.impls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.Task;
import ru.quperino.entities.UserTaskSolution;
import ru.quperino.entities.enums.SolutionStatus;
import ru.quperino.repositories.UserTaskSolutionRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Тесты генерации CSV-файла с историей решений.
 * Проверяются: наличие BOM, заголовки, экранирование спецсимволов,
 * корректный статус для разных типов решений.
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceImplTest {

    @Mock
    private UserTaskSolutionRepository solutionRepository;

    @InjectMocks
    private ExportServiceImpl exportService;

    private ApplicationUser testUser;

    @BeforeEach
    void setUp() {
        testUser = ApplicationUser.builder().id(1L).build();
    }

    /**
     * При отсутствии решений CSV должен содержать только BOM и заголовки.
     */
    @Test
    void generateExportCsv_shouldContainBomAndHeaders() {
        when(solutionRepository.findAllByUserOrderByCompletedAtDesc(testUser)).thenReturn(List.of());

        byte[] csvData = exportService.generateExportCsv(testUser);
        String content = new String(csvData, StandardCharsets.UTF_8);

        // Проверка BOM (0xEF, 0xBB, 0xBF)
        assertThat(csvData[0]).isEqualTo((byte) 0xEF);
        assertThat(csvData[1]).isEqualTo((byte) 0xBB);
        assertThat(csvData[2]).isEqualTo((byte) 0xBF);
        // После BOM идёт символ U+FEFF, затем заголовки
        assertThat(content).startsWith("\uFEFFНазвание занятия;Название задачи;Текст задачи;Статус;Решение пользователя;Дата;Ответ ИИ");
    }

    /**
     * Проверка, что успешно решённая задача из методички корректно экспортируется.
     */
    @Test
    void generateExportCsv_withCompletedMethodologyTask_shouldIncludeAllFields() {
        Task task = Task.builder()
                .id(10L)
                .section("METHODOLOGY")
                .lessonNumber(1)
                .lessonTitle("Занятие 1. Создание таблиц")
                .title("1.1. Создание таблицы")
                .text("Создайте таблицу publishers")
                .points(5)
                .build();

        UserTaskSolution solution = UserTaskSolution.builder()
                .user(testUser)
                .task(task)
                .status(SolutionStatus.COMPLETED)
                .lastCorrectSolution("CREATE TABLE publishers (id SERIAL PRIMARY KEY, name VARCHAR(200));")
                .aiFeedback("✅ Решение верно!")
                .completedAt(LocalDateTime.of(2025, 3, 15, 10, 30, 0))
                .attempts(2)
                .build();

        when(solutionRepository.findAllByUserOrderByCompletedAtDesc(testUser)).thenReturn(List.of(solution));

        byte[] csvData = exportService.generateExportCsv(testUser);
        String content = new String(csvData, StandardCharsets.UTF_8);

        assertThat(content).contains("Занятие 1. Создание таблиц");
        assertThat(content).contains("1.1. Создание таблицы");
        assertThat(content).contains("Создайте таблицу publishers");
        assertThat(content).contains("Решено верно");
        assertThat(content).contains("CREATE TABLE publishers");
        assertThat(content).contains("15.03.2025 10:30:00");
        assertThat(content).contains("✅ Решение верно!");
    }

    /**
     * Неудачное решение (FAILED) – решение пользователя не должно быть в CSV.
     */
    @Test
    void generateExportCsv_withFailedTask_shouldNotIncludeUserSolution() {
        Task task = Task.builder()
                .id(10L)
                .section("METHODOLOGY")
                .lessonTitle("Занятие 1")
                .title("1.2. Ошибка")
                .text("Неверный запрос")
                .build();

        UserTaskSolution solution = UserTaskSolution.builder()
                .user(testUser)
                .task(task)
                .status(SolutionStatus.FAILED)
                .lastCorrectSolution(null)
                .aiFeedback("❌ Синтаксическая ошибка")
                .completedAt(LocalDateTime.of(2025, 3, 15, 11, 0, 0))
                .attempts(3)
                .build();

        when(solutionRepository.findAllByUserOrderByCompletedAtDesc(testUser)).thenReturn(List.of(solution));

        byte[] csvData = exportService.generateExportCsv(testUser);
        String content = new String(csvData, StandardCharsets.UTF_8);

        assertThat(content).contains("Решено неверно");
        assertThat(content).doesNotContain("lastCorrectSolution");
        assertThat(content).contains("❌ Синтаксическая ошибка");
    }

    /**
     * Задача из секции ADVANCED: в колонке "Название занятия" должно быть "Повышенный уровень".
     */
    @Test
    void generateExportCsv_withAdvancedTask_shouldSetLessonTitleAsAdvanced() {
        Task task = Task.builder()
                .id(20L)
                .section("ADVANCED")
                .title("1.1. Топ-3 продукта")
                .text("Выберите топ-3")
                .build();

        UserTaskSolution solution = UserTaskSolution.builder()
                .user(testUser)
                .task(task)
                .status(SolutionStatus.COMPLETED)
                .lastCorrectSolution("SELECT name FROM products LIMIT 3")
                .aiFeedback("OK")
                .completedAt(LocalDateTime.now())
                .build();

        when(solutionRepository.findAllByUserOrderByCompletedAtDesc(testUser)).thenReturn(List.of(solution));

        byte[] csvData = exportService.generateExportCsv(testUser);
        String content = new String(csvData, StandardCharsets.UTF_8);

        assertThat(content).contains("Повышенный уровень");
    }

    /**
     * Экранирование спецсимволов: точки с запятой, кавычек, переводов строк.
     */
    @Test
    void generateExportCsv_withSpecialCharacters_shouldEscapeProperly() {
        Task task = Task.builder()
                .id(10L)
                .section("METHODOLOGY")
                .lessonTitle("Занятие 1")
                .title("Задача с ; кавычками \" и переносом\nстроки")
                .text("Текст; с \"кавычками\"\nи переносом")
                .build();

        UserTaskSolution solution = UserTaskSolution.builder()
                .user(testUser)
                .task(task)
                .status(SolutionStatus.COMPLETED)
                .lastCorrectSolution("SELECT * FROM \"books\" WHERE name = 'test';")
                .aiFeedback("Ответ с ; и \"кавычками\"")
                .completedAt(LocalDateTime.now())
                .build();

        when(solutionRepository.findAllByUserOrderByCompletedAtDesc(testUser)).thenReturn(List.of(solution));

        byte[] csvData = exportService.generateExportCsv(testUser);
        String content = new String(csvData, StandardCharsets.UTF_8);

        // Ожидаем, что поля с ;, " или \n обёрнуты в кавычки, а внутренние кавычки удвоены
        assertThat(content).contains("\"Задача с ; кавычками \"\" и переносом\nстроки\"");
        assertThat(content).contains("\"Текст; с \"\"кавычками\"\"\nи переносом\"");
        assertThat(content).contains("\"Ответ с ; и \"\"кавычками\"\"\"");
        // SQL-запрос с кавычками экранирован
        assertThat(content).contains("SELECT * FROM \"\"books\"\" WHERE name = 'test';");
    }

    /**
     * Отменённое решение (CANCELLED) – минимальный набор полей.
     */
    @Test
    void generateExportCsv_withCancelledTask_shouldExportMinimalInfo() {
        Task task = Task.builder()
                .id(10L)
                .section("METHODOLOGY")
                .lessonTitle("Занятие 1")
                .title("1.1. Отменённая задача")
                .text("Условие")
                .build();

        UserTaskSolution solution = UserTaskSolution.builder()
                .user(testUser)
                .task(task)
                .status(SolutionStatus.CANCELLED)
                .createdAt(LocalDateTime.of(2025, 3, 15, 9, 0, 0))
                .build();

        when(solutionRepository.findAllByUserOrderByCompletedAtDesc(testUser)).thenReturn(List.of(solution));

        byte[] csvData = exportService.generateExportCsv(testUser);
        String content = new String(csvData, StandardCharsets.UTF_8);

        assertThat(content).contains("Отменено");
        assertThat(content).doesNotContain("lastCorrectSolution");
        assertThat(content).doesNotContain("aiFeedback");
    }
}
