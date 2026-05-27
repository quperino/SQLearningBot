package ru.quperino.services.impls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.quperino.controllers.TelegramBotController;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.ButtonClick;
import ru.quperino.entities.Task;
import ru.quperino.entities.UserTaskSolution;
import ru.quperino.entities.enums.SolutionStatus;
import ru.quperino.entities.enums.UserStateEnum;
import ru.quperino.repositories.ButtonClickRepository;
import ru.quperino.repositories.UserTaskSolutionRepository;
import ru.quperino.services.HistoryService;
import ru.quperino.services.TaskService;
import ru.quperino.services.TasksMenuService;
import ru.quperino.services.UserService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Тесты {@link CallbackQueryProcessorServiceImpl} – обработка нажатий инлайн-кнопок.
 * Проверяется навигация по секциям, занятиям, задачам, тренировочный режим и сброс прогресса.
 */
@ExtendWith(MockitoExtension.class)
class CallbackQueryProcessorServiceImplTest {

    @Mock private TaskService taskService;
    @Mock private UserService userService;
    @Mock private UserTaskSolutionRepository solutionRepository;
    @Mock private ButtonClickRepository buttonClickRepository;
    @Mock private NodeUpdateProducerServiceImpl updateProducer;
    @Mock private TelegramBotController botController;
    @Mock private TasksMenuService tasksMenuService;
    @Mock private HistoryService historyService;

    @InjectMocks
    private CallbackQueryProcessorServiceImpl callbackProcessor;

    private CallbackQuery callbackQuery;
    private ApplicationUser testUser;
    private Task testTask;

    @BeforeEach
    void setUp() {
        callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        User fromUser = mock(User.class);

        lenient().when(callbackQuery.getMessage()).thenReturn(message);
        lenient().when(message.getChatId()).thenReturn(12345L);
        lenient().when(message.getMessageId()).thenReturn(100);
        lenient().when(callbackQuery.getFrom()).thenReturn(fromUser);
        lenient().when(fromUser.getId()).thenReturn(12345L);

        testUser = ApplicationUser.builder()
                .id(1L)
                .telegramUserId("12345")
                .userState(UserStateEnum.BASIC_STATE)
                .build();

        testTask = Task.builder()
                .id(200L)
                .section("METHODOLOGY")
                .title("1.1. Тестовая задача")
                .text("Условие задачи")
                .build();

        lenient().when(userService.findOrCreateUser(fromUser)).thenReturn(testUser);

        // Принудительно включаем кнопку сброса для теста
        ReflectionTestUtils.setField(callbackProcessor, "taskResetButtonEnabled", true);
    }

    /**
     * Выбор секции "METHODOLOGY": удаляем текущее сообщение и показываем список занятий.
     */
    @Test
    void processCallback_sectionMethodology_shouldDeleteMessageAndShowLessons() {
        when(callbackQuery.getData()).thenReturn("section_METHODOLOGY");
        callbackProcessor.processCallback(callbackQuery);
        verify(updateProducer).produceDeleteMessage(any(DeleteMessage.class));
        verify(tasksMenuService).sendLessonsBySection(12345L, "METHODOLOGY");
    }

    /**
     * Выбор секции "ADVANCED": удаляем текущее сообщение и показываем сразу задачи секции.
     */
    @Test
    void processCallback_sectionAdvanced_shouldShowTasksDirectly() {
        when(callbackQuery.getData()).thenReturn("section_ADVANCED");
        callbackProcessor.processCallback(callbackQuery);
        verify(updateProducer).produceDeleteMessage(any(DeleteMessage.class));
        verify(tasksMenuService).sendTasksBySection(12345L, "ADVANCED");
    }

    /**
     * Выбор занятия: удаляем сообщение, показываем список задач занятия.
     */
    @Test
    void processCallback_lesson_shouldShowTasksForLesson() {
        when(callbackQuery.getData()).thenReturn("lesson_5");
        callbackProcessor.processCallback(callbackQuery);
        verify(updateProducer).produceDeleteMessage(any(DeleteMessage.class));
        verify(tasksMenuService).sendTasksByLesson(12345L, "METHODOLOGY", 5);
    }

    /**
     * Выбор задачи, которая ещё не решена: создаётся новое решение, пользователь переводится
     * в состояние ожидания ввода SQL.
     */
    @Test
    void processCallback_task_notCompleted_shouldCreateSolutionAndShowTask() {
        when(callbackQuery.getData()).thenReturn("task_200");
        when(taskService.getTaskById(200L)).thenReturn(testTask);
        when(solutionRepository.existsByUserAndTaskAndStatus(testUser, testTask, SolutionStatus.COMPLETED))
                .thenReturn(false);
        when(solutionRepository.findByUserAndTask(testUser, testTask)).thenReturn(List.of());
        when(solutionRepository.findByUserAndStatus(testUser, SolutionStatus.PENDING))
                .thenReturn(List.of());

        callbackProcessor.processCallback(callbackQuery);

        verify(solutionRepository, atLeastOnce()).save(any(UserTaskSolution.class));
        verify(userService).updateUserState(testUser, UserStateEnum.WAIT_FOR_TASK_SOLUTION_STATE);
    }

    /**
     * Выбор задачи, которая уже решена: переключение в тренировочный режим (без баллов).
     */
    @Test
    void processCallback_task_alreadyCompleted_shouldEnterTrainingMode() {
        when(callbackQuery.getData()).thenReturn("task_200");
        when(taskService.getTaskById(200L)).thenReturn(testTask);
        when(solutionRepository.existsByUserAndTaskAndStatus(testUser, testTask, SolutionStatus.COMPLETED))
                .thenReturn(true);
        UserTaskSolution completed = UserTaskSolution.builder()
                .lastCorrectSolution("SELECT * FROM books")
                .attempts(3)
                .build();
        when(solutionRepository.findFirstByUserAndTaskAndStatusOrderByCompletedAtDesc(
                testUser, testTask, SolutionStatus.COMPLETED))
                .thenReturn(Optional.of(completed));

        callbackProcessor.processCallback(callbackQuery);

        verify(botController).sendAnswerMessageAndGetId(any(SendMessage.class));
        verify(userService).updateUserState(testUser, UserStateEnum.WAIT_FOR_TRAINING_SOLUTION_STATE);
        verify(userService).setTrainingTask(testUser, 200L);
    }

    /**
     * Кнопка возврата к списку секций.
     */
    @Test
    void processCallback_backToSections_shouldShowSectionsMenu() {
        when(callbackQuery.getData()).thenReturn("back_to_sections");
        callbackProcessor.processCallback(callbackQuery);
        verify(updateProducer).produceDeleteMessage(any(DeleteMessage.class));
        verify(updateProducer).producerAnswer(any(SendMessage.class));
    }

    /**
     * Кнопка возврата к списку занятий.
     */
    @Test
    void processCallback_backToLessons_shouldShowLessons() {
        when(callbackQuery.getData()).thenReturn("back_to_lessons");
        callbackProcessor.processCallback(callbackQuery);
        verify(updateProducer).produceDeleteMessage(any(DeleteMessage.class));
        verify(tasksMenuService).sendLessonsBySection(12345L, "METHODOLOGY");
    }

    /**
     * Сброс решения в тренировочном режиме.
     */
    @Test
    void processCallback_resetTask_shouldResetProgressAndShowLessons() {
        when(callbackQuery.getData()).thenReturn("reset_task_200");
        when(taskService.getTaskById(200L)).thenReturn(testTask);

        callbackProcessor.processCallback(callbackQuery);

        verify(userService).resetTaskProgress(testUser, 200L);
        verify(botController).sendAnswerCallbackQuery(any(), eq("Прогресс по задаче сброшен. Теперь её можно решить заново."));
        verify(updateProducer).produceDeleteMessage(any(DeleteMessage.class));
        verify(tasksMenuService).sendLessonsBySection(12345L, "METHODOLOGY");
    }

    /**
     * Пагинация истории (нажатие на кнопку "Далее" или "Назад").
     */
    @Test
    void processCallback_historyPage_shouldSendHistoryPage() {
        when(callbackQuery.getData()).thenReturn("history_page_2");
        callbackProcessor.processCallback(callbackQuery);
        verify(historyService).sendHistoryPage(testUser, 12345L, 2);
        verify(updateProducer).produceDeleteMessage(any(DeleteMessage.class));
    }

    /**
     * Закрытие истории – просто удаляем сообщение.
     */
    @Test
    void processCallback_historyClose_shouldDeleteMessage() {
        when(callbackQuery.getData()).thenReturn("history_close");
        callbackProcessor.processCallback(callbackQuery);
        verify(updateProducer).produceDeleteMessage(any(DeleteMessage.class));
        verifyNoMoreInteractions(updateProducer);
    }

    /**
     * Неизвестный callback – логируем предупреждение, ничего не делаем, но клик сохраняем.
     */
    @Test
    void processCallback_unknownData_shouldLogWarningAndDoNothing() {
        when(callbackQuery.getData()).thenReturn("unknown_data");
        callbackProcessor.processCallback(callbackQuery);
        verify(buttonClickRepository).save(any(ButtonClick.class));
        verify(botController).sendAnswerCallbackQuery(any(), isNull());
        verifyNoInteractions(tasksMenuService, historyService);
    }
}
