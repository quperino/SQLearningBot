package ru.quperino.services.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.quperino.dto.SolutionCheckRequest;
import ru.quperino.dto.SolutionCheckResponse;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.Task;
import ru.quperino.entities.UserTaskSolution;
import ru.quperino.entities.enums.SolutionStatus;
import ru.quperino.repositories.UserTaskSolutionRepository;
import ru.quperino.services.*;
import ru.quperino.util.JsonValidator;

import java.time.LocalDateTime;
import java.util.Optional;

import static ru.quperino.entities.enums.UserStateEnum.BASIC_STATE;
import static ru.quperino.model.RabbitQueue.*;

/**
 * Реализация консюмеров для очередей RabbitMQ в модуле Node.
 * <p>
 * Класс слушает три очереди:
 * <ul>
 *   <li>TEXT_MESSAGE_UPDATE – текстовые сообщения от пользователей</li>
 *   <li>CALLBACK_QUERY_UPDATE – нажатия инлайн-кнопок</li>
 *   <li>SOLUTION_CHECK_QUEUE – запросы на проверку SQL-решений</li>
 * </ul>
 *
 * Реализует интерфейсы:
 * {@link NodeAnswerConsumerService}, {@link AIConsumerService}, {@link CallbackConsumerService}
 */
@Service
@Log4j2
public class NodeAnswerConsumerServiceImpl implements NodeAnswerConsumerService, AIConsumerService, CallbackConsumerService {
    private final NodeMainService mainService;
    private final AIEvaluationService aiEvaluationService;
    private final CallbackQueryProcessorService callbackProcessor;
    private final NodeUpdateProducerServiceImpl updateProducer;
    private final UserTaskSolutionRepository solutionRepository;
    private final UserService userService;
    private final TasksMenuService tasksMenuService;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    @Autowired
    public NodeAnswerConsumerServiceImpl(NodeMainService mainService,
                                         AIEvaluationService aiEvaluationService,
                                         CallbackQueryProcessorService callbackProcessor,
                                         NodeUpdateProducerServiceImpl updateProducer,
                                         UserTaskSolutionRepository solutionRepository,
                                         UserService userService,
                                         TasksMenuService tasksMenuService,
                                         ObjectMapper objectMapper,
                                         TaskService taskService) {
        this.mainService = mainService;
        this.aiEvaluationService = aiEvaluationService;
        this.callbackProcessor = callbackProcessor;
        this.updateProducer = updateProducer;
        this.solutionRepository = solutionRepository;
        this.userService = userService;
        this.tasksMenuService = tasksMenuService;
        this.taskService = taskService;
        this.objectMapper = objectMapper;
    }

    /**
     * Обрабатывает текстовые сообщения из очереди TEXT_MESSAGE_UPDATE.
     * Десериализует JSON в объект Update и передаёт в NodeMainService.
     */
    @RabbitListener(queues = TEXT_MESSAGE_UPDATE)
    @Override
    public void consumeTextMessagesUpdates(String jsonUpdate) {
        log.debug("[NODE] Получено текстовое сообщение: {}", jsonUpdate);
        if (JsonValidator.isInvalidJson(jsonUpdate)) {
            log.warn("[NODE] Получен невалидный JSON в очереди TEXT_MESSAGE_UPDATE: {}", jsonUpdate);
            return;
        }
        try {
            Update update = objectMapper.readValue(jsonUpdate, Update.class);
            mainService.processTextMassage(update);
        } catch (Exception e) {
            log.error("[NODE] Ошибка десериализации Update из JSON", e);
        }
    }

    /**
     * Обрабатывает callback-запросы из очереди CALLBACK_QUERY_UPDATE.
     * Десериализует JSON в CallbackQuery и передаёт в CallbackQueryProcessorService.
     */
    @RabbitListener(queues = CALLBACK_QUERY_UPDATE)
    @Override
    public void consumeCallback(String jsonCallback) {
        log.debug("[NODE] consumeCallback вызван с JSON: {}", jsonCallback);
        if (JsonValidator.isInvalidJson(jsonCallback)) {
            log.warn("[NODE] Получен невалидный JSON в очереди CALLBACK_QUERY_UPDATE: {}", jsonCallback);
            return;
        }
        try {
            CallbackQuery callbackQuery = objectMapper.readValue(jsonCallback, CallbackQuery.class);
            callbackProcessor.processCallback(callbackQuery);
        } catch (Exception e) {
            log.error("[NODE] Ошибка десериализации CallbackQuery из JSON", e);
        }
    }

    /**
     * Обрабатывает запросы на проверку SQL-решений из очереди SOLUTION_CHECK_QUEUE.
     * <p>
     * Содержит полную логику для обычного и тренировочного режимов:
     * <ul>
     *   <li>Валидация JSON и десериализация в SolutionCheckRequest.</li>
     *   <li>Проверка существования активного решения в статусе PROCESSING.</li>
     *   <li>Вызов AIEvaluationService для получения вердикта.</li>
     *   <li>Начисление баллов (если успех и не тренировка).</li>
     *   <li>Обновление статуса решения и сохранение в БД.</li>
     *   <li>Формирование ответа пользователю и возврат в меню.</li>
     *   <li>Обработка тайм-аутов и ошибок.</li>
     * </ul>
     */
    @RabbitListener(queues = SOLUTION_CHECK_QUEUE)
    @Override
    public void consumeSolutionCheck(String jsonRequest) {
        log.debug("[NODE] Получен запрос на проверку решения: {}", jsonRequest);
        // 1. Валидация и десериализация
        if (JsonValidator.isInvalidJson(jsonRequest)) {
            log.warn("[NODE] Получен невалидный JSON в очереди SOLUTION_CHECK_QUEUE: {}", jsonRequest);
            return;
        }
        SolutionCheckRequest request = null;
        try {
            request = objectMapper.readValue(jsonRequest, SolutionCheckRequest.class);
            log.debug("[NODE] Проверка решения для задачи {}, тренировка: {}", request.getTaskId(), request.isTraining());

            // 2. Тренировочный режим
            if (request.isTraining()) {
                log.debug("[NODE] Тренировочная проверка для пользователя {}", request.getUserId());
                SolutionCheckResponse response = aiEvaluationService.evaluate(request);

                if ("timeout".equals(response.getStatus())) {
                    SendMessage timeoutMsg = new SendMessage();
                    timeoutMsg.setChatId(request.getChatId());
                    timeoutMsg.setText(response.getMessage());
                    updateProducer.producerAnswer(timeoutMsg);

                    ApplicationUser user = userService.findOrCreateUserById(request.getUserId());
                    if (user != null) {
                        userService.updateUserState(user, BASIC_STATE);
                        userService.clearTrainingTask(user);
                        sendBackToContext(request.getChatId(), request.getTaskId());
                    }
                    mainService.stopTypingIndicator(request.getChatId());
                    return;
                }

                String feedback = buildTrainingFeedback(response);
                SendMessage trainingMsg = new SendMessage();
                trainingMsg.setChatId(request.getChatId());
                trainingMsg.setText(feedback);
                updateProducer.producerAnswer(trainingMsg);

                if ("valid".equals(response.getStatus())) {
                    Task task = taskService.getTaskById(request.getTaskId());
                    ApplicationUser user = userService.findOrCreateUserById(request.getUserId());
                    Optional<UserTaskSolution> completedOpt = solutionRepository.findTopByUserAndTaskAndStatusOrderByCreatedAtDesc(user, task, SolutionStatus.COMPLETED);
                    if (completedOpt.isPresent()) {
                        UserTaskSolution completed = completedOpt.get();
                        completed.setLastCorrectSolution(request.getUserSolution());
                        solutionRepository.save(completed);
                        log.debug("[NODE] Обновлено последнее правильное решение в тренировочном режиме для задачи {}", request.getTaskId());
                    }
                }

                ApplicationUser user = userService.findOrCreateUserById(request.getUserId());
                if (user != null) {
                    userService.updateUserState(user, BASIC_STATE);
                    userService.clearTrainingTask(user);
                    sendBackToContext(request.getChatId(), request.getTaskId());
                }
                mainService.stopTypingIndicator(request.getChatId());
                return;
            }

            // 3. Обычный режим
            // Проверка: существует ли решение со статусом PROCESSING для этого пользователя и задачи
            if (solutionRepository.findByUser_IdAndTask_IdAndStatus(
                    request.getUserId(), request.getTaskId(), SolutionStatus.PROCESSING).isEmpty()) {
                log.debug("[NODE] Решение уже не в статусе PROCESSING, пропускаем ответ. Возможно, отменено.");
                mainService.stopTypingIndicator(request.getChatId());
                return;
            }

            // 4. Вызов AI
            SolutionCheckResponse response = aiEvaluationService.evaluate(request);
            boolean isSuccess = "valid".equals(response.getStatus());
            boolean isTimeout = "timeout".equals(response.getStatus());

            // 5. Повторная проверка существования решения в статусе PROCESSING (на случай отмены за время AI)
            Optional<UserTaskSolution> freshSolutionOpt = solutionRepository.findByUser_IdAndTask_IdAndStatus(
                    request.getUserId(), request.getTaskId(), SolutionStatus.PROCESSING);
            if (freshSolutionOpt.isEmpty()) {
                log.debug("[NODE] Решение было отменено во время проверки, ответ не отправляется.");
                mainService.stopTypingIndicator(request.getChatId());
                return;
            }
            UserTaskSolution freshSolution = freshSolutionOpt.get();

            // 6. Обработка таймаута
            if (isTimeout) {
                log.debug("[NODE] Таймаут проверки для задачи {}", request.getTaskId());
                freshSolution.setStatus(SolutionStatus.TIMEOUT);
                freshSolution.setAiFeedback(response.getMessage());
                freshSolution.setCompletedAt(LocalDateTime.now());
                solutionRepository.save(freshSolution);

                SendMessage timeoutMsg = new SendMessage();
                timeoutMsg.setChatId(request.getChatId());
                timeoutMsg.setText(response.getMessage());
                updateProducer.producerAnswer(timeoutMsg);

                sendBackToContext(request.getChatId(), request.getTaskId());

                ApplicationUser user = userService.findOrCreateUserById(request.getUserId());
                if (user != null) {
                    userService.updateUserState(user, BASIC_STATE);
                }
                mainService.stopTypingIndicator(request.getChatId());
                return;
            }

            // 7. Получение дополнительных данных (задача, пользователь)
            Task task = taskService.getTaskById(request.getTaskId());
            ApplicationUser user = userService.findOrCreateUserById(request.getUserId());
            boolean alreadyCompleted = solutionRepository.existsByUserAndTaskAndStatus(user, task, SolutionStatus.COMPLETED);

            // 8. Увеличение счётчика попыток (только если задача ещё не решена)
            if (!alreadyCompleted) {
                int currentAttempts = freshSolution.getAttempts() != null ? freshSolution.getAttempts() : 0;
                freshSolution.setAttempts(currentAttempts + 1);
                log.debug("[NODE] Увеличено количество попыток для решения id={}, новое значение={}", freshSolution.getId(), currentAttempts + 1);
            } else {
                log.debug("[NODE] Задача {} уже решена, попытки не увеличиваем", request.getTaskId());
            }

            // 9. Начисление баллов (если успех и задача не решена ранее)
            int pointsEarned = 0;
            int totalPoints = 0;
            if (isSuccess) {
                if (!alreadyCompleted) {
                    pointsEarned = task.getPoints();
                    userService.addPoints(user, pointsEarned);
                    totalPoints = user.getTotalPoints() != null ? user.getTotalPoints() : 0;
                    log.debug("[NODE] Начислено {} баллов пользователю {}. Всего: {}", pointsEarned, user.getId(), totalPoints);
                    freshSolution.setLastCorrectSolution(request.getUserSolution());
                } else {
                    // Аномалия: задача уже решена, но мы в обычном режиме. Отменяем решение.
                    // Проверка, которая вряд ли когда-то будет использована
                    String alreadyMsg = "✅ Задача уже была решена ранее. Баллы не начислены.";
                    SendMessage alreadyMessage = new SendMessage();
                    alreadyMessage.setChatId(request.getChatId());
                    alreadyMessage.setText(alreadyMsg);
                    updateProducer.producerAnswer(alreadyMessage);
                    freshSolution.setStatus(SolutionStatus.CANCELLED);
                    solutionRepository.save(freshSolution);
                    userService.updateUserState(user, BASIC_STATE);
                    mainService.stopTypingIndicator(request.getChatId());
                    return;
                }
            }

            // 10. Формирование текста ответа для пользователя
            String analysisText = buildAnalysisText(response, isSuccess, pointsEarned, totalPoints);
            SendMessage analysisMessage = new SendMessage();
            analysisMessage.setChatId(request.getChatId());
            analysisMessage.setText(analysisText);
            updateProducer.producerAnswer(analysisMessage);

            // 11. Обновление статуса решения
            freshSolution.setStatus(isSuccess ? SolutionStatus.COMPLETED : SolutionStatus.FAILED);
            freshSolution.setAiFeedback(analysisText);
            freshSolution.setCompletedAt(LocalDateTime.now());
            solutionRepository.save(freshSolution);
            log.debug("[NODE] Обновлена запись решения id={}, попытки={}", freshSolution.getId(), freshSolution.getAttempts());

            // 12. Завершение: сброс состояния пользователя, возврат в меню, остановка индикатора печати
            userService.updateUserState(user, BASIC_STATE);
            sendBackToContext(request.getChatId(), request.getTaskId());
            mainService.stopTypingIndicator(request.getChatId());

        } catch (Exception e) {
            log.error("[NODE] Ошибка при обработке запроса на проверку решения", e);
            try {
                if (request == null) {
                    log.error("[NODE] Не удалось десериализовать request, невозможно отправить сообщение об ошибке");
                    return;
                }

                if (request.isTraining()) {
                    SendMessage errorMsg = new SendMessage();
                    errorMsg.setChatId(request.getChatId());
                    errorMsg.setText("Техническая ошибка при проверке тренировочного решения.");
                    updateProducer.producerAnswer(errorMsg);
                    ApplicationUser user = userService.findOrCreateUserById(request.getUserId());
                    if (user != null) {
                        userService.updateUserState(user, BASIC_STATE);
                        userService.clearTrainingTask(user);
                        sendBackToContext(request.getChatId(), request.getTaskId());
                    }
                } else {
                    Optional<UserTaskSolution> solutionOpt = solutionRepository.findByUser_IdAndTask_IdAndStatus(
                            request.getUserId(), request.getTaskId(), SolutionStatus.PROCESSING);
                    if (solutionOpt.isPresent()) {
                        UserTaskSolution solution = solutionOpt.get();
                        solution.setStatus(SolutionStatus.FAILED);
                        solution.setAiFeedback("Техническая ошибка при проверке");
                        solution.setCompletedAt(LocalDateTime.now());
                        solutionRepository.save(solution);
                    }

                    String errorText = "Не удалось проверить решение из-за технической ошибки.";
                    SendMessage errorMsg = new SendMessage();
                    errorMsg.setChatId(request.getChatId());
                    errorMsg.setText(truncateText(errorText));
                    updateProducer.producerAnswer(errorMsg);

                    sendBackToContext(request.getChatId(), request.getTaskId());

                    ApplicationUser user = userService.findOrCreateUserById(request.getUserId());
                    if (user != null) {
                        userService.updateUserState(user, BASIC_STATE);
                    }
                }
                mainService.stopTypingIndicator(request.getChatId());
            } catch (Exception ex) {
                log.error("[NODE] Не удалось обработать ошибку проверки решения", ex);
            }
        }
    }

    /**
     * Формирует текст ответа для обычного режима (с баллами).
     */
    private String buildAnalysisText(SolutionCheckResponse response, boolean isSuccess, int pointsEarned, int totalPoints) {
        String base;
        if (isSuccess) {
            base = "✅ Решение верно!\n" + response.getMessage();
            if (response.getSuggestions() != null && !response.getSuggestions().isEmpty()) {
                base += "\n💡 Советы по улучшению: " + response.getSuggestions();
            }
            base += String.format("\n\n🏆 +%d баллов! Всего баллов у Вас: %d.", pointsEarned, totalPoints);
        } else {
            base = "❌ Решение неверно.\n" + response.getMessage();
            if (response.getSuggestions() != null && !response.getSuggestions().isEmpty()) {
                base += "\n" + response.getSuggestions();
            }
        }
        return truncateText(base);
    }

    /**
     * Формирует текст ответа для тренировочного режима (без баллов).
     */
    private String buildTrainingFeedback(SolutionCheckResponse response) {
        if ("valid".equals(response.getStatus())) {
            return "✅ Тренировка: решение верно! (баллы не начислены)\n" + response.getMessage();
        } else {
            return "❌ Тренировка: решение неверно.\n" + response.getMessage();
        }
    }

    /**
     * Обрезает слишком длинный текст до 4000 символов (лимит Telegram).
     */
    private String truncateText(String text) {
        if (text == null) return "";
        int MAX_LENGTH = 4000;
        if (text.length() <= MAX_LENGTH) return text;
        return text.substring(0, MAX_LENGTH - 3) + "...";
    }

    /**
     * Возвращает пользователя к списку задач или занятий после завершения проверки.
     */
    private void sendBackToContext(Long chatId, Long taskId) {
        Task task = taskService.getTaskById(taskId);
        if (task == null) {
            tasksMenuService.sendLessonsBySection(chatId, "METHODOLOGY");
            return;
        }
        String section = task.getSection();
        Integer lessonNumber = task.getLessonNumber();
        if ("METHODOLOGY".equals(section) && lessonNumber != null) {
            tasksMenuService.sendTasksByLesson(chatId, section, lessonNumber);
        } else if ("ADVANCED".equals(section)) {
            tasksMenuService.sendTasksBySection(chatId, section);
        } else {
            tasksMenuService.sendLessonsBySection(chatId, section);
        }
    }
}
