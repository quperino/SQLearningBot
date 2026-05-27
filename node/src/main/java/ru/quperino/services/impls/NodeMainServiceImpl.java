package ru.quperino.services.impls;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import ru.quperino.controllers.TelegramBotController;
import ru.quperino.dto.SolutionCheckRequest;
import ru.quperino.dto.UserStatistics;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.Task;
import ru.quperino.entities.UserTaskSolution;
import ru.quperino.entities.enums.SolutionStatus;
import ru.quperino.repositories.UserTaskSolutionRepository;
import ru.quperino.services.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

import static ru.quperino.entities.enums.UserStateEnum.*;
import static ru.quperino.services.enums.ServiceCommandsEnum.*;

/**
 * Реализация главного сервиса обработки сообщений.
 * <p>
 * Логика работы:
 * <ol>
 *   <li>При получении сообщения определяется текущее состояние пользователя (userState).</li>
 *   <li>Если сообщение является командой (например, /start), выполняется соответствующая обработка.</li>
 *   <li>Если пользователь в состоянии WAIT_FOR_EMAIL_STATE – проверяется корректность email и регистрируется.</li>
 *   <li>Если пользователь в состоянии WAIT_FOR_TASK_SOLUTION_STATE – отправленное сообщение считается SQL-запросом,
 *       создаётся запрос на проверку и отправляется в очередь SOLUTION_CHECK_QUEUE.</li>
 *   <li>Аналогично для WAIT_FOR_TRAINING_SOLUTION_STATE – тренировочная проверка.</li>
 *   <li>В состоянии BASIC_STATE любое нераспознанное сообщение игнорируется с подсказкой /help.</li>
 * </ol>
 */
@Service
@Log4j2
public class NodeMainServiceImpl implements NodeMainService {
    private final UserService userService;
    private final TaskService taskService;
    private final MessageService messageService;
    private final NodeUpdateProducerServiceImpl updateProducer;
    private final UserTaskSolutionRepository solutionRepository;
    private final CallbackQueryProcessorService callbackQueryProcessor;
    private final TelegramBotController telegramBotController;
    private final HistoryService historyService;
    private final ExportService exportService;
    private final MaterialService materialService;

    /**
     * Управление индикатором "печатает"
     */
    // Хранилище активных таймеров индикатора печати. Ключ – chatId, значение – запланированная задача.
    private final Map<Long, ScheduledFuture<?>> typingTimers = new ConcurrentHashMap<>();
    // Планировщик для периодической отправки индикатора "печатает".
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Флаг из конфигурации: разрешена ли команда /reset
    @Value("${app.features.reset-command.enabled:true}")
    private boolean resetCommandEnabled;

    @Autowired
    public NodeMainServiceImpl(UserService userService,
                               TaskService taskService,
                               MessageService messageService,
                               NodeUpdateProducerServiceImpl updateProducer,
                               UserTaskSolutionRepository solutionRepository,
                               CallbackQueryProcessorService callbackQueryProcessor,
                               TelegramBotController telegramBotController,
                               HistoryService historyService,
                               ExportService exportService,
                               MaterialService materialService) {
        this.userService = userService;
        this.taskService = taskService;
        this.messageService = messageService;
        this.updateProducer = updateProducer;
        this.solutionRepository = solutionRepository;
        this.callbackQueryProcessor = callbackQueryProcessor;
        this.telegramBotController = telegramBotController;
        this.historyService = historyService;
        this.exportService = exportService;
        this.materialService = materialService;
    }

    // ------------------------------------------------------------------------
    // Публичный метод – точка входа из консюмера
    // ------------------------------------------------------------------------

    /**
     * Обрабатывает входящее обновление (текст сообщения).
     * Определяет команду или текст, затем в зависимости от состояния пользователя
     * вызывает соответствующий приватный метод.
     *
     * @param update обновление от Telegram
     */
    @Override
    public void processTextMassage(Update update) {
        // засекаем время начала обработки
        long startTime = System.currentTimeMillis();

        // сообщение от пользователя
        var telegramMessage = update.getMessage();
        // кто отправил
        var telegramUser = telegramMessage.getFrom();
        // ID чата (у личного чата совпадает с userId)
        var chatId = telegramMessage.getChatId();
        // текст сообщения (может быть null, если не текст)
        var text = telegramMessage.getText();

        // удаляем лишние пробелы
        String trimmedText = text != null ? text.trim() : "";
        log.debug("[NODE] processTextMassage: получен текст: '{}'", trimmedText);

        // Находим или создаём пользователя в БД по данным из Telegram
        ApplicationUser user = userService.findOrCreateUser(telegramUser);
        // Сохраняем входящее сообщение в таблицу user_messages (для статистики). Время обработки пока 0, будет заполнено позже.
        messageService.saveUserMessage(user, telegramMessage, 0L);
        log.debug("[NODE] Текущее состояние пользователя: {}", user.getUserState());


        // --- Блок обработки команд (всегда имеют приоритет) ---
        if (trimmedText.equals(START.toString())) {
            log.debug("[NODE] Обработка команды /start от пользователя: {}", chatId);
            cancelActiveSolutionIfPresent(user, chatId);
            handleStart(user, chatId, startTime);
            return;
        }
        if (trimmedText.equals(RESET.toString())) {
            log.debug("[NODE] Обработка команды /reset от пользователя: {}", chatId);
            // если команда отключена в конфиге – сообщаем
            if (!resetCommandEnabled) {
                sendTextMessage(user, chatId, "Команда сброса отключена в текущей конфигурации.", startTime);
                return;
            }
            cancelActiveSolutionIfPresent(user, chatId);
            handleReset(user, chatId, startTime);
            return;
        }
        if (trimmedText.equals(CANCEL.toString())) {
            log.debug("[NODE] Обработка команды /cancel от пользователя: {}", chatId);
            cancelActiveSolutionIfPresent(user, chatId);
            handleCancel(user, chatId, startTime);
            return;
        }
        if (trimmedText.equals(HELP.toString())) {
            log.debug("[NODE] Обработка команды /help от пользователя: {}", chatId);
            cancelActiveSolutionIfPresent(user, chatId);
            sendHelp(user, chatId, startTime);
            return;
        }
        if (trimmedText.equals(REGISTRATION.toString())) {
            log.debug("[NODE] Обработка команды /registration от пользователя: {}", chatId);
            cancelActiveSolutionIfPresent(user, chatId);
            userService.updateUserState(user, WAIT_FOR_EMAIL_STATE);
            sendTextMessage(user, chatId, "Введите ваш email для регистрации:", startTime);
            return;
        }
        if (trimmedText.equals(STATS.toString())) {
            log.debug("[NODE] Обработка команды /stats от пользователя: {}", chatId);
            if (user.getUserState() != BASIC_STATE) {
                sendTextMessage(user, chatId, "Команда /stats недоступна во время решения задачи. Завершите или отмените текущее действие.", startTime);
                return;
            }
            handleStats(user, chatId, startTime);
            return;
        }
        if (trimmedText.equals(HISTORY.toString())) {
            log.debug("[NODE] Обработка команды /history от пользователя: {}", chatId);
            if (user.getUserState() != BASIC_STATE) {
                sendTextMessage(user, chatId, "Команда /history недоступна во время решения задачи. Завершите или отмените текущее действие.", startTime);
                return;
            }
            handleHistory(user, chatId, startTime);
            return;
        }
        if (trimmedText.equals(EXPORT.toString())) {
            log.debug("[NODE] Обработка команды /export от пользователя: {}", chatId);
            if (user.getUserState() != BASIC_STATE) {
                sendTextMessage(user, chatId, "Команда /export недоступна во время решения задачи. Завершите или отмените текущее действие.", startTime);
                return;
            }
            handleExport(user, chatId, startTime);
            return;
        }
        if (trimmedText.equals(HINT.toString())) {
            log.debug("[NODE] Обработка команды /hint от пользователя: {}", chatId);
            if (user.getUserState() != BASIC_STATE) {
                sendTextMessage(user, chatId, "Команда /hint доступна только во время решения задачи.", startTime);
                return;
            }
            handleHint(user, chatId, startTime);
            return;
        }
        if (trimmedText.equals(MATERIALS.toString())) {
            log.debug("[NODE] Обработка команды /materials от пользователя: {}", chatId);
            if (user.getUserState() != BASIC_STATE) {
                sendTextMessage(user, chatId, "Команда /materials недоступна во время решения задачи. Завершите или отмените текущее действие.", startTime);
                return;
            }
            handleMaterials(user, chatId, startTime);
            return;
        }

        // Дополнительная защита: если пользователь в WAIT_FOR_TASK_SOLUTION_STATE, но активного решения нет – сброс
        if (user.getUserState() == WAIT_FOR_TASK_SOLUTION_STATE) {
            Optional<UserTaskSolution> activeSolution = findActiveSolution(user);
            if (activeSolution.isEmpty()) {
                log.debug("[NODE] Нет активного решения, сбрасываем состояние в BASIC_STATE");
                userService.updateUserState(user, BASIC_STATE);
                sendTextMessage(user, chatId, "Сессия решения задачи истекла. Выберите задачу из меню.", startTime);
                callbackQueryProcessor.sendSectionsMenu(chatId.toString());
                return;
            }
        }

        // --- Основной switch по состоянию пользователя ---
        switch (user.getUserState()) {
            case BASIC_STATE:
                sendTextMessage(user, chatId, "Неизвестная команда. Введите /help.", startTime);
                break;
            case WAIT_FOR_EMAIL_STATE:
                handleEmailRegistration(user, trimmedText, chatId, startTime);
                break;
            case WAIT_FOR_TASK_SOLUTION_STATE:
                handleSolutionSubmission(user, trimmedText, chatId, startTime);
                break;
            case WAIT_FOR_TRAINING_SOLUTION_STATE:
                handleTrainingSolutionSubmission(user, trimmedText, chatId, startTime);
                break;
            default:
                sendTextMessage(user, chatId, "Неизвестная ошибка. Введите /cancel.", startTime);
        }
    }

    // ------------------------------------------------------------------------
    // Приватные методы – реализация конкретных команд и состояний
    // ------------------------------------------------------------------------

    /**
     * Обработка команды /start.
     * Если email отсутствует – переводим в состояние ожидания email.
     * Если email есть – показываем главное меню секций.
     */
    private void handleStart(ApplicationUser user, Long chatId, long startTime) {
        log.debug("[NODE] handleStart: сброс сессии, email сохраняется");
        // Если пользователь уже в состоянии ожидания email – просто напоминаем.
        if (user.getUserState() == WAIT_FOR_EMAIL_STATE) {
            sendTextMessage(user, chatId, "Введите ваш email для регистрации:", startTime);
            return;
        }
        // Очищаем сессию: отменяем активные решения, сбрасываем состояние.
        userService.clearUserSession(user);
        // Нет email – переводим в состояние ожидания email
        if (user.getEmail() == null) {
            userService.updateUserState(user, WAIT_FOR_EMAIL_STATE);
            sendTextMessage(user, chatId, "Введите ваш email для регистрации:", startTime);
        } else {
            // Email есть – показываем главное меню секций
            callbackQueryProcessor.sendSectionsMenu(chatId.toString());
        }
    }

    /**
     * Обработка команды /reset – полный сброс данных пользователя.
     * Удаляются все решения, сообщения, баллы, email. Пользователь становится как новый.
     */
    private void handleReset(ApplicationUser user, Long chatId, long startTime) {
        log.debug("[NODE] handleReset: полный сброс данных пользователя");
        // Удаляем все решения, сообщения, баллы, email и т.д.
        userService.resetUserData(user);
        // После сброса пользователь как новый – переводим в состояние ожидания email
        userService.updateUserState(user, WAIT_FOR_EMAIL_STATE);
        sendTextMessage(user, chatId, "Выполнен полный сброс. Введите email для регистрации:", startTime);
    }

    /**
     * Поиск активного решения (PENDING или PROCESSING).
     * Используется для проверки, есть ли у пользователя незавершённая задача.
     */
    private Optional<UserTaskSolution> findActiveSolution(ApplicationUser user) {
        // Сначала ищем решение со статусом PENDING (ждущее ввода SQL)
        Optional<UserTaskSolution> pending = solutionRepository.findTopByUserAndStatusOrderByCreatedAtDesc(user, SolutionStatus.PENDING);
        if (pending.isPresent()) return pending;
        // Если нет PENDING, ищем со статусом PROCESSING (отправлено на проверку, ответ не получен)
        return solutionRepository.findTopByUserAndStatusOrderByCreatedAtDesc(user, SolutionStatus.PROCESSING);
    }

    /**
     * Обработка команды /cancel – отмена текущего действия.
     * Отменяет активное решение (если есть), сбрасывает состояние и показывает главное меню.
     */
    private void handleCancel(ApplicationUser user, Long chatId, long startTime) {
        // Останавливаем индикатор "печатает", если он активен (например, при PROCESSING)
        stopTypingIndicator(chatId); // останавливаем периодическую отправку "печатает", если была

        // Ищем, есть ли активное решение: если есть - отменяем
        Optional<UserTaskSolution> active = findActiveSolution(user);
        if (active.isPresent()) {
            UserTaskSolution solution = active.get();
            SolutionStatus oldStatus = solution.getStatus();
            // Меняем статус на CANCELLED (отменено)
            solution.setStatus(SolutionStatus.CANCELLED);
            solutionRepository.save(solution);
            // Отправляем разное сообщение в зависимости от того, ждало ли решение проверки
            if (oldStatus == SolutionStatus.PROCESSING) {
                sendTextMessage(user, chatId, "Проверка решения отменена.", startTime);
            } else {
                sendTextMessage(user, chatId, "Команда отменена.", startTime);
            }
        } else {
            // Если активного решения не было – просто сообщаем об отмене
            sendTextMessage(user, chatId, "Команда отменена.", startTime);
        }
        // Сбрасываем состояние пользователя в базовое
        userService.updateUserState(user, BASIC_STATE);
        // Показываем главное меню секций
        callbackQueryProcessor.sendSectionsMenu(chatId.toString());
    }

    /**
     * Обработка ввода email в состоянии WAIT_FOR_EMAIL_STATE.
     * Проверяет валидность email через регулярное выражение и сохраняет.
     */
    private void handleEmailRegistration(ApplicationUser user, String email, Long chatId, long startTime) {
        if (userService.isEmailValid(email)) {
            userService.registerEmail(user, email);
            callbackQueryProcessor.sendSectionsMenu(chatId.toString());
        } else {
            sendTextMessage(user, chatId, "Некорректный email. Попробуйте ещё раз.", startTime);
        }
    }

    /**
     * Обработка отправки решения в обычном режиме (WAIT_FOR_TASK_SOLUTION_STATE).
     * Отправляет запрос на проверку в очередь SOLUTION_CHECK_QUEUE.
     */
    private void handleSolutionSubmission(ApplicationUser user, String solutionText, Long chatId, long startTime) {
        // Проверяем наличие активного решения
        var active = findActiveSolution(user);
        if (active.isEmpty()) {
            sendTextMessage(user, chatId, "У вас нет активной задачи. Выберите задачу из меню.", startTime);
            userService.updateUserState(user, BASIC_STATE);
            callbackQueryProcessor.sendSectionsMenu(chatId.toString());
            return;
        }

        UserTaskSolution solution = active.get();
        Task task = solution.getTask();

        // Если решение уже в статусе PROCESSING (уже проверяется) – не даём отправить ещё раз
        if (solution.getStatus() == SolutionStatus.PROCESSING) {
            sendTextMessage(user, chatId, "Ваше решение уже проверяется, подождите.", startTime);
            return;
        }

        // Меняем статус на PROCESSING и сохраняем в БД
        solution.setStatus(SolutionStatus.PROCESSING);
        solutionRepository.save(solution);

        // Убираем инлайн-клавиатуру (кнопки "Подсказка", "Назад") у сообщения с условием задачи
        Integer taskMessageId = solution.getTaskMessageId();
        if (taskMessageId != null) {
            updateProducer.produceEditMessageReplyMarkup(chatId, taskMessageId);
        }

        // Сообщаем пользователю, что решение принято в обработку
        sendTextMessage(user, chatId, "Решение принято! Проверяется...", startTime);
        // Запускаем индикатор "печатает" (будет периодически отправляться, пока не остановим)
        startTypingIndicator(chatId);

        // Формируем запрос на проверку и отправляем в очередь SOLUTION_CHECK_QUEUE
        updateProducer.produceSolutionCheck(new SolutionCheckRequest(
                task.getId(), task.getText(), solutionText, chatId, user.getId(), null, false));
    }

    /**
     * Обработка отправки решения в тренировочном режиме (WAIT_FOR_TRAINING_SOLUTION_STATE).
     * Аналогична обычной проверке, но с флагом training = true и без начисления баллов.
     */
    private void handleTrainingSolutionSubmission(ApplicationUser user, String solutionText, Long chatId, long startTime) {
        // Получаем ID задачи, которую пользователь тренирует (сохранён в поле trainingTaskId)
        Long taskId = userService.getTrainingTask(user);
        if (taskId == null) {
            sendTextMessage(user, chatId, "Ошибка: не выбрана задача для тренировки. Начните заново.", startTime);
            userService.updateUserState(user, BASIC_STATE);
            userService.clearTrainingTask(user);
            callbackQueryProcessor.sendSectionsMenu(chatId.toString());
            return;
        }

        Task task = taskService.getTaskById(taskId);
        if (task == null) {
            sendTextMessage(user, chatId, "Задача не найдена.", startTime);
            userService.updateUserState(user, BASIC_STATE);
            userService.clearTrainingTask(user);
            callbackQueryProcessor.sendSectionsMenu(chatId.toString());
            return;
        }

        // Убеждаемся, что задача действительно решена пользователем (защита от багов)
        boolean alreadyCompleted = solutionRepository.existsByUserAndTaskAndStatus(user, task, SolutionStatus.COMPLETED);
        if (!alreadyCompleted) {
            sendTextMessage(user, chatId, "Эта задача ещё не решена. Пожалуйста, решите её в обычном режиме.", startTime);
            userService.updateUserState(user, BASIC_STATE);
            userService.clearTrainingTask(user);
            callbackQueryProcessor.sendSectionsMenu(chatId.toString());
            return;
        }

        // Получаем messageId тренировочного сообщения (сохранён в карте)
        Integer taskMessageId = CallbackQueryProcessorServiceImpl.getAndRemoveTrainingMessageId(chatId.toString(), task.getId());
        if (taskMessageId != null) {
            // Удаляем клавиатуру у тренировочного сообщения (кнопки "Назад", "Сбросить решение")
            updateProducer.produceEditMessageReplyMarkup(chatId, taskMessageId);
        }

        sendTextMessage(user, chatId, "🔄 Проверяем тренировочное решение...", startTime);
        startTypingIndicator(chatId);
        updateProducer.produceSolutionCheck(new SolutionCheckRequest(
                task.getId(), task.getText(), solutionText, chatId, user.getId(), null, true));
    }

    /**
     * Обработка команды /stats – вывод статистики пользователя.
     * Использует UserService для сбора данных о решённых задачах и баллах.
     */
    private void handleStats(ApplicationUser user, Long chatId, long startTime) {
        // Получаем объект со статистикой через UserService
        UserStatistics stats = userService.getUserStatistics(user);
        StringBuilder sb = new StringBuilder();
        sb.append("📊 **Ваша статистика** 📊\n\n");
        sb.append("🏆 Всего баллов: ").append(stats.getPoints()).append("\n\n");

        // Секция "Методичка" – выводим статистику по каждому занятию
        sb.append("📚 **СЕКЦИЯ «МЕТОДИЧКА»**\n");
        List<UserStatistics.LessonStats> lessonStats = stats.getMethodologyLessonStats();
        if (lessonStats.isEmpty()) {
            sb.append("Нет решённых задач.\n");
        } else {
            for (UserStatistics.LessonStats ls : lessonStats) {
                sb.append(ls.getLessonTitle()).append("\n");
                sb.append("   Решено задач: ").append(ls.getSolvedTasks())
                        .append(". Количество баллов: ").append(ls.getEarnedPoints()).append("\n");
            }
        }

        // Секция "Повышенный уровень" – итоговые цифры
        sb.append("\n⭐ **СЕКЦИЯ «ПОВЫШЕННЫЙ УРОВЕНЬ»**\n");
        sb.append("Решено задач: ").append(stats.getAdvancedTotalSolved())
                .append(". Количество баллов: ").append(stats.getAdvancedTotalPoints()).append("\n");

        sb.append("\nДля получения подробной статистики воспользуйтесь командой /export.");

        sendTextMessage(user, chatId, sb.toString(), startTime);
    }

    /**
     * Обработка команды /history – отправка первого экрана истории решений с пагинацией.
     */
    private void handleHistory(ApplicationUser user, Long chatId, long startTime) {
        historyService.sendHistoryPage(user, chatId, 0);
        log.debug("[NODE] Обработка команды /history заняла {} мс", System.currentTimeMillis() - startTime);
    }

    /**
     * Обработка команды /export – формирование CSV-файла и отправка пользователю.
     */
    private void handleExport(ApplicationUser user, Long chatId, long startTime) {
        if (user.getUserState() != BASIC_STATE) {
            sendTextMessage(user, chatId, "Команда /export недоступна во время решения задачи. Завершите или отмените текущее решение.", startTime);
            return;
        }
        sendTextMessage(user, chatId, "Формируем CSV-файл с историей решений...", startTime);
        byte[] csvData = exportService.generateExportCsv(user);
        if (csvData.length == 0) {
            sendTextMessage(user, chatId, "Не удалось сформировать файл экспорта.", startTime);
            return;
        }
        // Имя файла: history_<userId>_<время>.csv
        String fileName = "history_" + user.getId() + "_" + System.currentTimeMillis() + ".csv";
        // Отправляем документ через producer
        updateProducer.produceDocument(csvData, fileName, chatId.toString());
    }

    /**
     * Обработка команды /hint – отправка подсказки по текущей активной задаче.
     */
    private void handleHint(ApplicationUser user, Long chatId, long startTime) {
        // Подсказка доступна только в состояниях ожидания решения (обычное или тренировочное)
        if (user.getUserState() != WAIT_FOR_TASK_SOLUTION_STATE && user.getUserState() != WAIT_FOR_TRAINING_SOLUTION_STATE) {
            sendTextMessageKeepKeyboard(user, chatId, "Подсказка доступна только после выбора задачи.", startTime);
            return;
        }
        Task task;
        if (user.getUserState() == WAIT_FOR_TASK_SOLUTION_STATE) {
            Optional<UserTaskSolution> active = findActiveSolution(user);
            if (active.isEmpty()) {
                sendTextMessageKeepKeyboard(user, chatId, "Активная задача не найдена.", startTime);
                return;
            }
            task = active.get().getTask();
        } else {
            Long taskId = userService.getTrainingTask(user);
            if (taskId == null) {
                sendTextMessageKeepKeyboard(user, chatId, "Задача не найдена.", startTime);
                return;
            }
            task = taskService.getTaskById(taskId);
            if (task == null) {
                sendTextMessageKeepKeyboard(user, chatId, "Задача не найдена.", startTime);
                return;
            }
        }
        String hint = task.getHintText();
        if (hint == null || hint.isBlank()) {
            sendTextMessageKeepKeyboard(user, chatId, "Для этой задачи подсказка не добавлена. Ожидается решение...", startTime);
            return;
        }
        markHintUsed(user, task);
        sendTextMessageKeepKeyboard(user, chatId, "💡 Подсказка:\n" + hint, startTime);
    }

    /**
     * Обработка команды /materials – отправка PDF-файла методического пособия.
     */
    private void handleMaterials(ApplicationUser user, Long chatId, long startTime) {
        if (user.getEmail() == null) {
            sendTextMessage(user, chatId, "Для получения материалов сначала зарегистрируйтесь: /start", startTime);
            return;
        }
        if (user.getUserState() != BASIC_STATE) {
            sendTextMessage(user, chatId, "Команда недоступна во время решения задачи. Завершите или отмените текущее действие.", startTime);
            return;
        }
        try {
            sendTextMessage(user, chatId, "📚 Загружаю методический материал...", startTime);
            byte[] pdfData = materialService.getMaterialsPdf();
            String fileName = "Методическое пособие \"PostgreSQL с нуля\".pdf";
            updateProducer.produceDocument(pdfData, fileName, chatId.toString());
        } catch (IOException e) {
            log.error("[NODE] Ошибка чтения PDF-файла", e);
            sendTextMessage(user, chatId, "Не удалось загрузить материалы. Попробуйте позже.", startTime);
        }
    }

    /**
     * Отмечает факт использования подсказки в соответствующем решении.
     */
    private void markHintUsed(ApplicationUser user, Task task) {
        Optional<UserTaskSolution> solutionOpt;
        if (user.getUserState() == WAIT_FOR_TASK_SOLUTION_STATE) {
            solutionOpt = findActiveSolution(user);
        } else {
            solutionOpt = solutionRepository.findFirstByUserAndTaskAndStatusOrderByCompletedAtDesc(user, task, SolutionStatus.COMPLETED);
        }
        if (solutionOpt.isPresent()) {
            UserTaskSolution solution = solutionOpt.get();
            if (!solution.getHintUsed()) {
                solution.setHintUsed(true);
                solutionRepository.save(solution);
                log.debug("[NODE] Пользователь {} использовал подсказку для задачи {}", user.getId(), task.getId());
            }
        }
    }

    /**
     * Отправляет справочное сообщение со списком команд.
     */
    private void sendHelp(ApplicationUser user, Long chatId, long startTime) {
        sendTextMessage(user, chatId, """
                /start – начать
                /reset – полный сброс
                /help – помощь
                /cancel – отмена
                /registration – регистрация/смена email
                /stats – моя статистика
                /history – история решений
                /export – экспорт в CSV
                /hint – подсказка по задаче
                /materials – получить методическое пособие (PDF)""", startTime);
    }


    // ------------------------------------------------------------------------
    // Вспомогательные методы для отправки сообщений и управления индикатором
    // ------------------------------------------------------------------------

    /**
     * Отправляет текстовое сообщение и удаляет стандартную клавиатуру (ReplyKeyboardRemove).
     */
    private void sendTextMessage(ApplicationUser user, Long chatId, String text, long startTime) {
        SendMessage msg = new SendMessage(chatId.toString(), text);
        ReplyKeyboardRemove removeKeyboard = new ReplyKeyboardRemove();
        removeKeyboard.setRemoveKeyboard(true);
        msg.setReplyMarkup(removeKeyboard);
        updateProducer.producerAnswer(msg);
        if (user != null) {
            long processingTime = System.currentTimeMillis() - startTime;
            messageService.saveBotMessage(user, text, processingTime);
            log.debug("[NODE] Время обработки сообщения для пользователя {}: {} мс", user.getId(), processingTime);
        }
    }

    /**
     * Отправляет текстовое сообщение без удаления стандартной клавиатуры (используется для подсказок).
     */
    private void sendTextMessageKeepKeyboard(ApplicationUser user, Long chatId, String text, long startTime) {
        SendMessage msg = new SendMessage(chatId.toString(), text);
        updateProducer.producerAnswer(msg);
        if (user != null) {
            long processingTime = System.currentTimeMillis() - startTime;
            messageService.saveBotMessage(user, text, processingTime);
            log.debug("[NODE] Время обработки сообщения (keep keyboard) для пользователя {}: {} мс", user.getId(), processingTime);
        }
    }

    /**
     * Запускает периодическую отправку действия "печатает" (каждые 10 секунд).
     */
    private void startTypingIndicator(Long chatId) {
        stopTypingIndicator(chatId);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() ->
                telegramBotController.sendChatAction(chatId.toString()), 0, 10, TimeUnit.SECONDS);
        typingTimers.put(chatId, future);
    }

    /**
     * Останавливает индикатор "печатает" для чата.
     */
    @Override
    public void stopTypingIndicator(Long chatId) {
        ScheduledFuture<?> future = typingTimers.remove(chatId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    /**
     * Отменяет активное решение (если есть) и удаляет сообщение с условием задачи.
     * Возвращает true, если решение было отменено.
     */
    private boolean cancelActiveSolutionIfPresent(ApplicationUser user, Long chatId) {
        stopTypingIndicator(chatId); // останавливаем индикатор, если он активен

        Optional<UserTaskSolution> active = findActiveSolution(user);
        if (active.isEmpty()) {
            return false;
        }
        UserTaskSolution solution = active.get();
        Integer taskMessageId = solution.getTaskMessageId();
        // Удаляем сообщение с условием задачи (чтобы не висело мёртвым грузом)
        if (taskMessageId != null) {
            updateProducer.produceDeleteMessage(new DeleteMessage(chatId.toString(), taskMessageId));
        }
        // Меняем статус на CANCELLED
        solution.setStatus(SolutionStatus.CANCELLED);
        solutionRepository.save(solution);
        // Сбрасываем состояние и тренировочную задачу
        userService.updateUserState(user, BASIC_STATE);
        userService.clearTrainingTask(user);
        log.debug("[NODE] Отменено активное решение {} для пользователя {}", solution.getId(), user.getId());
        return true;
    }
}
