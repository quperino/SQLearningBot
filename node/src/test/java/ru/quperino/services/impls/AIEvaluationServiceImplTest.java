package ru.quperino.services.impls;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.quperino.dto.SolutionCheckResponse;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты приватного метода parseGeneratedText {@link AIEvaluationServiceImpl},
 * который парсит ответ AI в структурированный объект.
 * <p>
 * Проверяются различные форматы ответов AI: чистый JSON, Markdown-обёртка,
 * текст с лишними словами до/после JSON, fallback-разбор.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class AIEvaluationServiceImplTest {

    private AIEvaluationServiceImpl aiEvaluationService;

    @BeforeEach
    void setUp() throws Exception {
        // Создаём ObjectMapper, аналогичный тому, что в AppConfig
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(new JavaTimeModule());
        aiEvaluationService = new AIEvaluationServiceImpl(objectMapper);

        // Устанавливаем поля apiUrl и modelName через рефлексию, чтобы избежать NPE при логировании
        var apiUrlField = AIEvaluationServiceImpl.class.getDeclaredField("apiUrl");
        apiUrlField.setAccessible(true);
        apiUrlField.set(aiEvaluationService, "http://dummy");
        var modelNameField = AIEvaluationServiceImpl.class.getDeclaredField("modelName");
        modelNameField.setAccessible(true);
        modelNameField.set(aiEvaluationService, "dummy-model");
    }

    /**
     * Вызывает приватный метод parseGeneratedText через рефлексию.
     */
    private SolutionCheckResponse parseGeneratedText(String text) throws Exception {
        Method method = AIEvaluationServiceImpl.class.getDeclaredMethod("parseGeneratedText", String.class);
        method.setAccessible(true);
        return (SolutionCheckResponse) method.invoke(aiEvaluationService, text);
    }

    /**
     * Чистый JSON с корректными полями → status = "valid".
     */
    @Test
    void parseGeneratedText_validJson_shouldReturnValidStatus() throws Exception {
        String response = "{\"status\":\"valid\",\"message\":\"Запрос верен\",\"suggestions\":\"\"}";
        SolutionCheckResponse result = parseGeneratedText(response);
        assertThat(result.getStatus()).isEqualTo("valid");
        assertThat(result.getMessage()).isEqualTo("Запрос верен");
        assertThat(result.getSuggestions()).isEmpty();
    }

    /**
     * Чистый JSON со статусом "invalid".
     */
    @Test
    void parseGeneratedText_invalidJson_shouldReturnInvalidStatus() throws Exception {
        String response = "{\"status\":\"invalid\",\"message\":\"Синтаксическая ошибка\",\"suggestions\":\"Проверьте SELECT\"}";
        SolutionCheckResponse result = parseGeneratedText(response);
        assertThat(result.getStatus()).isEqualTo("invalid");
        assertThat(result.getMessage()).isEqualTo("Синтаксическая ошибка");
        assertThat(result.getSuggestions()).isEqualTo("Проверьте SELECT");
    }

    /**
     * Ответ в Markdown: ```json ... ``` → должен быть извлечён и распарсен.
     */
    @Test
    void parseGeneratedText_responseWithMarkdown_shouldCleanAndParse() throws Exception {
        String response = "```json\n{\"status\":\"valid\",\"message\":\"Верно\",\"suggestions\":\"\"}\n```";
        SolutionCheckResponse result = parseGeneratedText(response);
        assertThat(result.getStatus()).isEqualTo("valid");
        assertThat(result.getMessage()).isEqualTo("Верно");
    }

    /**
     * Текст, содержащий JSON, и дополнительные слова до/после → JSON должен быть извлечён.
     */
    @Test
    void parseGeneratedText_responseWithExtraTextBeforeJson_shouldExtractJson() throws Exception {
        String response = "Some text before {\"status\":\"valid\",\"message\":\"OK\",\"suggestions\":\"\"} and after";
        SolutionCheckResponse result = parseGeneratedText(response);
        assertThat(result.getStatus()).isEqualTo("valid");
        assertThat(result.getMessage()).isEqualTo("OK");
    }

    /**
     * Ответ без JSON, но содержащий подсказку "valid" → fallback должен вернуть valid.
     */
    @Test
    void parseGeneratedText_noJson_shouldFallbackToTextParsing() throws Exception {
        String response = "This is a valid SQL query. It will work.";
        SolutionCheckResponse result = parseGeneratedText(response);
        assertThat(result.getStatus()).isEqualTo("valid");
        assertThat(result.getMessage()).isEqualTo("This is a valid SQL query. It will work.");
    }

    /**
     * Ответ без JSON, но содержащий слово "invalid" → fallback вернёт invalid.
     */
    @Test
    void parseGeneratedText_noJson_withInvalidKeyword_shouldReturnInvalid() throws Exception {
        String response = "This is wrong. Invalid syntax.";
        SolutionCheckResponse result = parseGeneratedText(response);
        assertThat(result.getStatus()).isEqualTo("invalid");
        assertThat(result.getMessage()).isEqualTo("This is wrong. Invalid syntax.");
    }

    /**
     * null или пустая строка → возвращается invalid с сообщением об ошибке.
     */
    @Test
    void parseGeneratedText_nullInput_shouldReturnInvalid() throws Exception {
        SolutionCheckResponse result = parseGeneratedText(null);
        assertThat(result.getStatus()).isEqualTo("invalid");
        assertThat(result.getMessage()).isEqualTo("AI не вернул ответ.");
    }

    @Test
    void parseGeneratedText_emptyString_shouldReturnInvalid() throws Exception {
        SolutionCheckResponse result = parseGeneratedText("");
        assertThat(result.getStatus()).isEqualTo("invalid");
        assertThat(result.getMessage()).isEqualTo("AI не вернул ответ.");
    }

    /**
     * Проверка формирования системного промпта и пользовательского сообщения.
     * Метод buildMessages должен создавать список из двух элементов (system, user).
     */
    @Test
    void buildMessages_shouldContainSchemaAndTaskAndSolution() throws Exception {
        Method buildMessages = AIEvaluationServiceImpl.class.getDeclaredMethod("buildMessages", String.class, String.class);
        buildMessages.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) buildMessages.invoke(aiEvaluationService, "task text", "user solution");

        assertThat(messages).hasSize(2);
        Map<String, String> systemMsg = messages.get(0);
        assertThat(systemMsg.get("role")).isEqualTo("system");
        assertThat(systemMsg.get("content")).contains("Схема базы данных \"Library\"");

        Map<String, String> userMsg = messages.get(1);
        assertThat(userMsg.get("role")).isEqualTo("user");
        assertThat(userMsg.get("content")).contains("ЗАДАЧА: task text");
        assertThat(userMsg.get("content")).contains("ПОЛЬЗОВАТЕЛЬСКИЙ SQL:\nuser solution");
    }
}
