package ru.quperino.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс приложения. Регистрирует бины, общие для всех модулей.
 */
@Configuration
public class AppConfig {
    /**
     * Создаёт и настраивает ObjectMapper для сериализации/десериализации JSON.
     * <ul>
     *   <li>Отключает fail on unknown properties – игнорирует неизвестные поля при десериализации.</li>
     *   <li>Регистрирует модуль для работы с Java 8+ Date/Time API (LocalDateTime, LocalDate).</li>
     * </ul>
     * Этот ObjectMapper используется в RabbitMQ‑продюсерах и консюмерах.
     *
     * @return настроенный экземпляр ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
