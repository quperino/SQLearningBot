package ru.quperino.integration;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import ru.quperino.controllers.BotInitializerController;
import ru.quperino.controllers.TelegramBotController;
import ru.quperino.services.AIEvaluationService;

/**
 * Базовый класс для всех интеграционных тестов приложения.
 * 
 * Настройки:
 *   {@code @SpringBootTest(webEnvironment = NONE)} – запускает Spring-контекст без веб-сервера.
 *   {@code @ActiveProfiles("test")} – использует конфигурацию из application-test.properties (H2 in-memory).
 *   {@code @Transactional} – каждая тестовая транзакция откатывается после теста, сохраняя БД чистой.
 * 
 * Заглушки (MockitoBean) для внешних зависимостей:
 *   {@link RabbitTemplate} – отправка в RabbitMQ (не нужна в тестах).
 *   {@link TelegramBotController} – реальные вызовы Telegram API не производятся.
 *   {@link AIEvaluationService} – AI API не вызывается (в тестах используется заглушка).
 *   {@link BotInitializerController} – инициализация бота не требуется.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {
    @MockitoBean
    protected RabbitTemplate rabbitTemplate;

    @MockitoBean
    protected TelegramBotController telegramBotController;

    @MockitoBean
    protected AIEvaluationService aiEvaluationService;

    @MockitoBean
    protected BotInitializerController botInitializerController;
}
