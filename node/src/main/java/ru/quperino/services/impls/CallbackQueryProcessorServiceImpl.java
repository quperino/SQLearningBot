package ru.quperino.services.impls;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.MaybeInaccessibleMessage;
import ru.quperino.controllers.TelegramBotController;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.ButtonClick;
import ru.quperino.entities.Task;
import ru.quperino.entities.UserTaskSolution;
import ru.quperino.entities.enums.SolutionStatus;
import ru.quperino.repositories.ButtonClickRepository;
import ru.quperino.repositories.UserTaskSolutionRepository;
import ru.quperino.services.*;
import ru.quperino.util.KeyboardHelperUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static ru.quperino.entities.enums.UserStateEnum.*;

/**
 * Реализация {@link CallbackQueryProcessorService}.
 * Обрабатывает нажатия на inline-кнопки: навигация по секциям/занятиям/задачам,
 * запуск тренировочного режима, сброс прогресса, подсказки, пагинация истории.
 */
@Service
@Log4j2
public class CallbackQueryProcessorServiceImpl implements CallbackQueryProcessorService {
    private final TaskService taskService;
    private final UserService userService;
    private final UserTaskSolutionRepository solutionRepository;
    private final ButtonClickRepository buttonClickRepository;
    private final NodeUpdateProducerServiceImpl updateProducer;
    private final TelegramBotController botController;
    private final TasksMenuService tasksMenuService;
    private final HistoryService historyService;

    // Статическая карта для хранения messageId тренировочных сообщений.
    // Ключ: строка "chatId_taskId", значение: messageId.
    // Нужна, чтобы потом убрать клавиатуру (кнопки) из тренировочного сообщения.
    private static final Map<String, Integer> trainingMessageIds = new ConcurrentHashMap<>();

    // Кэши текущей секции и занятия для каждого чата (используются для навигации "Назад")
    private final Map<Long, String> currentSectionForChat = new ConcurrentHashMap<>();
    private final Map<Long, Integer> currentLessonForChat = new ConcurrentHashMap<>();

    // Флаг из конфигурации: включена ли кнопка "Сбросить решение" в тренировочном режиме
    @Value("${app.features.task-reset-button.enabled:true}")
    private boolean taskResetButtonEnabled;

    @Autowired
    public CallbackQueryProcessorServiceImpl(TaskService taskService,
                                             UserService userService,
                                             UserTaskSolutionRepository solutionRepository,
                                             ButtonClickRepository buttonClickRepository,
                                             NodeUpdateProducerServiceImpl updateProducer,
                                             TelegramBotController botController,
                                             TasksMenuService tasksMenuService,
                                             HistoryService historyService) {
        this.taskService = taskService;
        this.userService = userService;
        this.solutionRepository = solutionRepository;
        this.buttonClickRepository = buttonClickRepository;
        this.updateProducer = updateProducer;
        this.botController = botController;
        this.tasksMenuService = tasksMenuService;
        this.historyService = historyService;
    }

    /**
     * Публичный статический метод для получения и удаления messageId тренировочного сообщения.
     * Используется в NodeMainServiceImpl, чтобы убрать клавиатуру после отправки решения.
     */
    public static Integer getAndRemoveTrainingMessageId(String chatId, Long taskId) {
        return trainingMessageIds.remove(chatId + "_" + taskId);
    }

    // Основной метод обработки callback-запросов (нажатий инлайн-кнопок)
    @Override
    public void processCallback(CallbackQuery callbackQuery) {
        log.debug("[NODE] Получены данные callback: '{}'", callbackQuery.getData());
        // строка, которую мы передали в setCallbackData
        String data = callbackQuery.getData();
        // сообщение, к которому была прикреплена кнопка
        MaybeInaccessibleMessage originalMessage = callbackQuery.getMessage();
        // пользователь, нажавший кнопку
        ApplicationUser user = userService.findOrCreateUser(callbackQuery.getFrom());

        // Сохраняем факт нажатия кнопки в таблицу button_clicks (для аналитики)
        ButtonClick click = ButtonClick.builder()
                .user(user)
                .buttonName(data)
                .createdAt(LocalDateTime.now())
                .build();
        // Сохраняем клик в статистику
        buttonClickRepository.save(click);

        // Обязательно отвечаем на callback-запрос, чтобы Telegram не показывал ошибку
        // Второй параметр null – не показываем всплывающее уведомление.
        botController.sendAnswerCallbackQuery(callbackQuery.getId(), null);

        // Извлекаем chatId и messageId сообщения, к которому привязана кнопка
        String chatId;
        Integer messageId = null;
        // Если сообщение недоступно (редкий случай), используем userId как chatId
        if (originalMessage == null) {
            chatId = callbackQuery.getFrom().getId().toString();
            log.warn("[NODE] CallbackQuery без сообщения, используем userId как chatId: {}", chatId);
        } else {
            chatId = originalMessage.getChatId().toString();
            messageId = originalMessage.getMessageId();
        }
        Long chatIdLong = Long.parseLong(chatId);

        log.debug("[NODE] processCallback: data={}, chatId={}, messageId={}, userState={}",
                data, chatId, messageId, user.getUserState());

        // Вспомогательный объект для сброса состояния пользователя при навигации
        Runnable resetState = () -> {
            if (user.getUserState() == WAIT_FOR_TASK_SOLUTION_STATE || user.getUserState() == WAIT_FOR_TRAINING_SOLUTION_STATE) {
                userService.updateUserState(user, BASIC_STATE);
                userService.clearTrainingTask(user);
                log.debug("[NODE] Сброшено состояние пользователя {} с {} на BASIC_STATE", user.getId(), user.getUserState());
            }
        };

        // --- Выбор секции ---
        if (data.startsWith("section_")) {
            // отрезаем "section_", получаем "METHODOLOGY" или "ADVANCED"
            // Удаляем текущее сообщение (меню секций), чтобы не захламлять чат
            String section = data.substring(8);
            if (messageId != null) {
                updateProducer.produceDeleteMessage(new DeleteMessage(chatId, messageId));
            }
            // сброс перед сменой секции
            resetState.run();
            // запоминаем выбранную секцию для чата
            currentSectionForChat.put(chatIdLong, section);
            if ("METHODOLOGY".equals(section)) {
                // Для методички – показываем список занятий
                tasksMenuService.sendLessonsBySection(chatIdLong, section);
            } else if ("ADVANCED".equals(section)) {
                // Для повышенного уровня – получаем все задачи и показываем сразу (без занятий)
                tasksMenuService.sendTasksBySection(chatIdLong, section);
            }
            return;
        }

        // --- Выбор занятия (только для METHODOLOGY) ---
        if (data.startsWith("lesson_")) {
            Integer lessonNumber = Integer.parseInt(data.substring(7));
            String section = currentSectionForChat.get(chatIdLong);
            if (section == null) section = "METHODOLOGY";
            resetState.run(); // сброс
            // запоминаем выбранное занятие
            currentLessonForChat.put(chatIdLong, lessonNumber);
            if (messageId != null) {
                updateProducer.produceDeleteMessage(new DeleteMessage(chatId, messageId));
            }
            // Показываем список задач для данного занятия
            tasksMenuService.sendTasksByLesson(chatIdLong, section, lessonNumber);
            return;
        }

        // --- Выбор задачи ---
        if (data.startsWith("task_")) {
            // Разрешаем выбор только в базовом состоянии или если уже ожидаем решение задачи
            if (user.getUserState() != BASIC_STATE && user.getUserState() != WAIT_FOR_TASK_SOLUTION_STATE) {
                log.debug("[NODE] Некорректное состояние {} для выбора задачи. Удаляем сообщение.", user.getUserState());
                if (messageId != null) {
                    updateProducer.produceDeleteMessage(new DeleteMessage(chatId, messageId));
                }
                return;
            }

            Long taskId = Long.parseLong(data.substring(5));
            Task task = taskService.getTaskById(taskId);
            if (task == null) {
                log.error("[NODE] Задача с id {} не найдена", taskId);
                return;
            }

            if (messageId != null) {
                updateProducer.produceDeleteMessage(new DeleteMessage(chatId, messageId));
            }

            // Проверка на уже решённую задачу (тренировочный режим)
            boolean alreadyCompleted = solutionRepository.existsByUserAndTaskAndStatus(user, task, SolutionStatus.COMPLETED);
            if (alreadyCompleted) {
                botController.sendAnswerCallbackQuery(callbackQuery.getId(), "Задача уже решена. Режим тренировки (баллы не начисляются).");

                String lastSolution = "";
                int attempts = 0;
                Optional<UserTaskSolution> lastCompleted = solutionRepository.findFirstByUserAndTaskAndStatusOrderByCompletedAtDesc(user, task, SolutionStatus.COMPLETED);
                if (lastCompleted.isPresent()) {
                    UserTaskSolution completed = lastCompleted.get();
                    if (completed.getLastCorrectSolution() != null) {
                        lastSolution = completed.getLastCorrectSolution();
                    }
                    attempts = completed.getAttempts() != null ? completed.getAttempts() : 0;
                }

                String trainingMessageText =
                        "⚠️ РЕЖИМ ТРЕНИРОВКИ\n" +
                                "Вы уже решили эту задачу ранее. Повторное решение НЕ принесёт баллов, но вы можете отправить SQL для самопроверки.\n\n" +
                                "📌 Условие задачи:\n" + task.getText() + "\n\n" +
                                "✅ Ваше последнее правильное решение:\n" + lastSolution + "\n\n" +
                                "🔢 Количество попыток до решения: " + attempts + "\n\n" +
                                "✏️ Отправьте свой SQL-запрос для проверки.";

                SendMessage trainingMessage = new SendMessage(chatId, trainingMessageText);
                String section = task.getSection();
                Integer lessonNumber = "METHODOLOGY".equals(section) ? task.getLessonNumber() : null;
                trainingMessage.setReplyMarkup(KeyboardHelperUtil.createTrainingModeKeyboard(task.getId(), section, lessonNumber));
                Integer msgId = botController.sendAnswerMessageAndGetId(trainingMessage);
                if (msgId != null) {
                    trainingMessageIds.put(chatId + "_" + task.getId(), msgId);
                }

                userService.updateUserState(user, WAIT_FOR_TRAINING_SOLUTION_STATE);
                userService.setTrainingTask(user, task.getId());
                return;
            }

            // --- Обычный режим (задача не решена) ---
            // Удаляем старые решения по этой задаче
            List<UserTaskSolution> oldSolutions = solutionRepository.findByUserAndTask(user, task);
            for (UserTaskSolution old : oldSolutions) {
                solutionRepository.delete(old);
                log.debug("[NODE] Удалена старая запись решения id={} для задачи {}", old.getId(), taskId);
            }

            // Определяем секцию и номер занятия для клавиатуры
            String section = task.getSection();
            Integer lessonNumber = null;
            if ("METHODOLOGY".equals(section)) {
                lessonNumber = currentLessonForChat.get(chatIdLong);
                if (lessonNumber == null) {
                    lessonNumber = task.getLessonNumber();
                }
            }

            // Создаём новое решение
            UserTaskSolution solution = UserTaskSolution.builder()
                    .user(user)
                    .task(task)
                    .status(SolutionStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .attempts(0)
                    .build();
            solutionRepository.save(solution);

            // Отправляем сообщение с задачей и получаем messageId
            Integer msgId = sendTaskWithInlineKeyboard(chatId, task, taskId, section, lessonNumber);
            if (msgId != null) {
                solution.setTaskMessageId(msgId);
                solutionRepository.save(solution);
            }

            // Отменяем любые другие висящие PENDING решения
            List<UserTaskSolution> pendingSolutions = solutionRepository.findByUserAndStatus(user, SolutionStatus.PENDING);
            for (UserTaskSolution sol : pendingSolutions) {
                if (!sol.getId().equals(solution.getId())) {
                    sol.setStatus(SolutionStatus.CANCELLED);
                    solutionRepository.save(sol);
                }
            }

            userService.updateUserState(user, WAIT_FOR_TASK_SOLUTION_STATE);
            log.debug("[NODE] Состояние пользователя после выбора задачи: {}", user.getUserState());
            return;
        }

        // --- Обработка подсказки ---
        if (data.startsWith("hint_")) {
            Long taskId = Long.parseLong(data.substring(5));
            handleHint(user, chatIdLong, taskId);
            botController.sendAnswerCallbackQuery(callbackQuery.getId(), null);
            return;
        }

        // --- Возврат к списку задач занятия (для METHODOLOGY) ---
        if (data.startsWith("back_to_tasks_")) {
            String[] parts = data.split("_");
            if (parts.length >= 5) {
                String section = parts[3];
                Integer lessonNumber = Integer.parseInt(parts[4]);
                resetState.run(); // сброс
                if (messageId != null) {
                    updateProducer.produceDeleteMessage(new DeleteMessage(chatId, messageId));
                }
                tasksMenuService.sendTasksByLesson(chatIdLong, section, lessonNumber);
                botController.sendAnswerCallbackQuery(callbackQuery.getId(), null);
                return;
            }
        }

        // --- Устаревший back_to_tasks (совместимость) ---
        if ("back_to_tasks".equals(data)) {
            resetState.run();
            if (messageId != null) {
                updateProducer.produceDeleteMessage(new DeleteMessage(chatId, messageId));
            }
            String section = currentSectionForChat.get(chatIdLong);
            if (section == null) section = "METHODOLOGY";
            tasksMenuService.sendLessonsBySection(chatIdLong, section);
            botController.sendAnswerCallbackQuery(callbackQuery.getId(), null);
            return;
        }

        // --- Возврат к списку задач секции (ADVANCED) ---
        if (data.startsWith("back_to_section_")) {
            String section = data.substring(16);
            resetState.run();
            if (messageId != null) {
                updateProducer.produceDeleteMessage(new DeleteMessage(chatId, messageId));
            }
            tasksMenuService.sendTasksBySection(chatIdLong, section);
            botController.sendAnswerCallbackQuery(callbackQuery.getId(), null);
            return;
        }

        // --- Возврат к списку секций ---
        if ("back_to_sections".equals(data)) {
            resetState.run();
            if (messageId != null) {
                updateProducer.produceDeleteMessage(new DeleteMessage(chatId, messageId));
            }
            sendSectionsMenu(chatId);
            return;
        }

        // --- Возврат к списку занятий (для METHODOLOGY) ---
        if ("back_to_lessons".equals(data)) {
            resetState.run();
            if (messageId != null) {
                updateProducer.produceDeleteMessage(new DeleteMessage(chatId, messageId));
            }
            String section = currentSectionForChat.get(chatIdLong);
            if (section == null) section = "METHODOLOGY";
            tasksMenuService.sendLessonsBySection(chatIdLong, section);
            return;
        }

        // --- Сброс решения (тренировочный режим) ---
        if (data.startsWith("reset_task_")) {
            if (!taskResetButtonEnabled) {
                botController.sendAnswerCallbackQuery(callbackQuery.getId(), "Функция сброса временно недоступна.");
                SendMessage infoMsg = new SendMessage(chatId, "⚠️ Сброс решения недоступен в текущей конфигурации бота.");
                updateProducer.producerAnswer(infoMsg);
                return;
            }
            String[] parts = data.split("_");
            if (parts.length < 3) {
                log.warn("[NODE] Invalid reset_task callback: {}", data);
                return;
            }
            long taskId;
            try {
                taskId = Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                log.warn("[NODE] Failed to parse taskId from {}", data);
                return;
            }

            // Извлекаем lessonNumber, если присутствует
            Integer lessonNumber = null;
            if (parts.length >= 4) {
                try {
                    lessonNumber = Integer.parseInt(parts[3]);
                } catch (NumberFormatException e) {
                    log.warn("[NODE] Не удалось спарсить lessonNumber из {}", data);
                }
            }

            Task task = taskService.getTaskById(taskId);
            if (task == null) {
                log.error("[NODE] Задача с id {} не найдена для сброса", taskId);
                return;
            }

            if (messageId != null) {
                updateProducer.produceDeleteMessage(new DeleteMessage(chatId, messageId));
            }

            userService.resetTaskProgress(user, taskId);
            botController.sendAnswerCallbackQuery(callbackQuery.getId(), "Прогресс по задаче сброшен. Теперь её можно решить заново.");

            // Возвращаемся в правильный контекст
            String section = task.getSection();
            if (lessonNumber != null) {
                // Для METHODOLOGY (или другой секции с занятиями) – возвращаемся к списку задач занятия
                tasksMenuService.sendTasksByLesson(chatIdLong, section, lessonNumber);
            } else if ("ADVANCED".equals(section)) {
                // Для ADVANCED – возвращаемся к списку задач секции
                tasksMenuService.sendTasksBySection(chatIdLong, section);
            } else {
                // На всякий случай – возвращаемся к списку занятий
                tasksMenuService.sendLessonsBySection(chatIdLong, section);
            }
            return;
        }

        // --- Обработка пагинации истории ---
        if (data.startsWith("history_page_")) {
            int page = Integer.parseInt(data.substring(13));
            historyService.sendHistoryPage(user, Long.parseLong(chatId), page);
            if (messageId != null) {
                updateProducer.produceDeleteMessage(new DeleteMessage(chatId, messageId));
            }
            return;
        }
        if ("history_close".equals(data)) {
            if (messageId != null) {
                updateProducer.produceDeleteMessage(new DeleteMessage(chatId, messageId));
            }
            return;
        }

        log.warn("[NODE] Неизвестный callback: {}", data);
    }

    @Override
    public void sendSectionsMenu(String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Выберите раздел задач:");
        sendMessage.setReplyMarkup(KeyboardHelperUtil.createSectionsKeyboard());
        updateProducer.producerAnswer(sendMessage);
    }

    /**
     * Отправляет задачу с inline-клавиатурой (Подсказка, Назад).
     */
    private Integer sendTaskWithInlineKeyboard(String chatId, Task task, Long taskId, String section, Integer lessonNumber) {
        String text = task.getTitle() + "\n\n" + task.getText();
        SendMessage message = new SendMessage(chatId, text);
        message.setReplyMarkup(KeyboardHelperUtil.createTaskSolvingKeyboard(taskId, section, lessonNumber));
        return botController.sendAnswerMessageAndGetId(message);
    }

    /**
     * Обрабатывает запрос подсказки: отправляет текст подсказки и отмечает её использование.
     */
    private void handleHint(ApplicationUser user, Long chatId, Long taskId) {
        Task task = taskService.getTaskById(taskId);
        if (task == null) {
            updateProducer.producerAnswer(new SendMessage(chatId.toString(), "Задача не найдена."));
            return;
        }
        String hint = task.getHintText();
        if (hint == null || hint.isBlank()) {
            updateProducer.producerAnswer(new SendMessage(chatId.toString(), "Для этой задачи подсказка не добавлена."));
            return;
        }
        // Отметить использование подсказки, если есть активное решение
        Optional<UserTaskSolution> activeSolution = solutionRepository.findTopByUserAndStatusOrderByCreatedAtDesc(user, SolutionStatus.PENDING);
        if (activeSolution.isPresent()) {
            UserTaskSolution solution = activeSolution.get();
            if (!solution.getHintUsed() && solution.getTask().getId().equals(taskId)) {
                solution.setHintUsed(true);
                solutionRepository.save(solution);
            }
        }
        updateProducer.producerAnswer(new SendMessage(chatId.toString(), "💡 Подсказка:\n" + hint));
    }
}
