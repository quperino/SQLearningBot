package ru.quperino.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест утилитарного класса {@link JsonValidator}.
 * Проверяет корректность определения синтаксически невалидного JSON.
 * <p>
 * Валидным считается JSON, начинающийся с '{' или '[', который может быть
 * успешно прочитан парсером Jackson (хотя бы до первого токена).
 * </p>
 */
class JsonValidatorTest {

    /**
     * Валидный JSON-объект или массив НЕ должен считаться невалидным.
     * <p>
     * Проверяем простой объект и массив чисел.
     * </p>
     */
    @Test
    void isInvalidJson_withValidObject_shouldReturnFalse() {
        assertThat(JsonValidator.isInvalidJson("{\"key\":\"value\"}")).isFalse();
        assertThat(JsonValidator.isInvalidJson("[1,2,3]")).isFalse();
    }

    /**
     * Передача {@code null} должна возвращать {@code true} (невалидный JSON).
     */
    @Test
    void isInvalidJson_withNull_shouldReturnTrue() {
        assertThat(JsonValidator.isInvalidJson(null)).isTrue();
    }

    /**
     * Пустая строка или строка из одних пробелов должна считаться невалидной.
     */
    @Test
    void isInvalidJson_withBlank_shouldReturnTrue() {
        assertThat(JsonValidator.isInvalidJson("")).isTrue();
        assertThat(JsonValidator.isInvalidJson("   ")).isTrue();
    }

    /**
     * Текст, который не начинается с '{' или '[' и не является синтаксически корректным JSON,
     * должен считаться невалидным.
     */
    @Test
    void isInvalidJson_withInvalidSyntax_shouldReturnTrue() {
        assertThat(JsonValidator.isInvalidJson("plain text")).isTrue();
    }

    /**
     * Пустой объект {@code {}} и пустой массив {@code []} являются формально валидным JSON.
     * Ожидается, что метод вернёт {@code false}.
     */
    @Test
    void isInvalidJson_withEmptyObject_shouldReturnFalse() {
        assertThat(JsonValidator.isInvalidJson("{}")).isFalse();
        assertThat(JsonValidator.isInvalidJson("[]")).isFalse();
    }
}
