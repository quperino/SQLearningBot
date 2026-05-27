package ru.quperino.controllers;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Инициализатор Telegram-бота.
 * <p>
 * Выполняется после запуска Spring-контекста (метод init).
 * Регистрирует бота в Telegram API и устанавливает меню команд.
 */
@Component
@Log4j2
public class BotInitializerController {
    private final TelegramBotController telegramBotController;

    @Autowired
    public BotInitializerController(TelegramBotController telegramBotController) {
        this.telegramBotController = telegramBotController;
    }

    /**
     * Инициализация бота при старте приложения.
     * <ol>
     *   <li>Регистрирует бота в Telegram через TelegramBotsApi.</li>
     *   <li>Устанавливает меню команд (список доступных команд с описанием).</li>
     * </ol>
     */
    @PostConstruct
    public void init() {
        // Регистрация бота
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBotController);
            log.info("[DISPATCHER] Бот успешно зарегистрирован");
        } catch (TelegramApiException e) {
            log.error("[DISPATCHER] Ошибка регистрации бота", e);
        }

        // Установка меню команд (отображается при вводе "/" в Telegram)
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("start", "Начать работу/регистрация"));
        commands.add(new BotCommand("help", "Справка по командам"));
        commands.add(new BotCommand("cancel", "Отменить текущее действие"));
        commands.add(new BotCommand("reset", "Полный сброс прогресса"));
        commands.add(new BotCommand("registration", "Регистрация/смена Email"));
        commands.add(new BotCommand("stats", "Моя статистика"));
        commands.add(new BotCommand("history", "История решений"));
        commands.add(new BotCommand("export", "Экспорт в CSV"));
        commands.add(new BotCommand("hint", "Подсказка к задаче"));
        commands.add(new BotCommand("materials", "Методические материалы"));

        try {
            telegramBotController.execute(new SetMyCommands(commands, null, null));
            log.info("[DISPATCHER] Меню команд Telegram успешно установлено.");
        } catch (TelegramApiException e) {
            log.error("[DISPATCHER] Не удалось установить меню команд", e);
        }
    }
}
