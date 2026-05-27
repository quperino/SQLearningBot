package ru.quperino.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

/**
 * Утилитарный класс для быстрой проверки, является ли строка синтаксически корректным JSON.
 * Используется перед десериализацией сообщений из очередей RabbitMQ,
 * чтобы избежать лишних исключений при заведомо невалидных данных.
 */
@Log4j2
public class JsonValidator {
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    /**
     * Проверяет, является ли переданная строка невалидным JSON.
     * Валидным считается JSON, который начинается с '{' или '['
     * и может быть успешно прочитан парсером (хотя бы первый токен).
     *
     * @param json строка для проверки (может быть null)
     * @return {@code true}, если JSON невалиден (null, пуст, не начинается с {{ или [, или содержит синтаксические ошибки)
     */
    public static boolean isInvalidJson(String json) {
        if (json == null || json.isBlank()) {
            log.warn("JSON равен null или пуст");
            return true;
        }
        String trimmed = json.trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            log.warn("JSON не начинается с '{{' или '[': {}", trimmed.length() > 100 ? trimmed.substring(0, 100) + "..." : trimmed);
            return true;
        }
        try (JsonParser parser = JSON_FACTORY.createParser(json)) {
            // Попытка прочитать первый токен (например, START_OBJECT или START_ARRAY)
            JsonToken firstToken = parser.nextToken();
            if (firstToken == null) {
                log.warn("JSON пуст");
                return true;
            }
            // Синтаксис корректен, парсер не выбросил исключение
            return false;
        } catch (IOException e) {
            log.warn("Ошибка разбора JSON: {}", e.getMessage());
            return true;
        }
    }
}
