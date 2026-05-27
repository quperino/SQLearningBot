package ru.quperino;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главный класс Spring Boot приложения "SQLearningBot".
 * <ul>
 *   <li>Сканирует пакет ru.quperino (все модули: app, dispatcher, node, common‑jpa, common‑rabbitmq).</li>
 *   <li>Включает поддержку RabbitMQ через аннотацию {@link EnableRabbit}.</li>
 *   <li>Логирует запуск и проверяет наличие Flyway в classpath.</li>
 * </ul>
 */
@EnableRabbit
@SpringBootApplication(scanBasePackages = "ru.quperino")
@Log4j2
public class SQLearningBotApplication {
    public static void main(String[] args) {
        log.info("Приложение запускается... Log4j2 инициализирован");
        SpringApplication.run(SQLearningBotApplication.class, args);
    }

    /**
     * Выполняется после создания контекста, но до старта сервера.
     * Проверяет, что Flyway (миграции БД) присутствует в classpath.
     * В случае отсутствия логирует ошибку – это помогает диагностировать проблемы со сборкой.
     */
    @PostConstruct
    public void checkFlyway() {
        try {
            Class.forName("org.flywaydb.core.Flyway");
            log.info("Flyway class found");
        } catch (ClassNotFoundException e) {
            log.error("Flyway class NOT found", e);
        }
    }
}
