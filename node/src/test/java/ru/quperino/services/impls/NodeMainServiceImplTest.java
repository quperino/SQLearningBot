package ru.quperino.services.impls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.quperino.controllers.TelegramBotController;
import ru.quperino.dto.SolutionCheckRequest;
import ru.quperino.dto.UserStatistics;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.Task;
import ru.quperino.entities.UserTaskSolution;
import ru.quperino.entities.enums.SolutionStatus;
import ru.quperino.entities.enums.UserStateEnum;
import ru.quperino.repositories.UserTaskSolutionRepository;
import ru.quperino.services.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Тесты {@link NodeMainServiceImpl} – главного сервиса обработки сообщений.
 * Проверяется обработка команд, смена состояний, отправка решений на проверку.
 */
@ExtendWith(MockitoExtension.class)
class NodeMainServiceImplTest {

    @Mock private UserService userService;
    @Mock private TaskService taskService;
    @Mock private MessageService messageService;
    @Mock private NodeUpdateProducerServiceImpl updateProducer;
    @Mock private UserTaskSolutionRepository solutionRepository;
    @Mock private CallbackQueryProcessorService callbackQueryProcessor;
    @Mock private TelegramBotController telegramBotController;
    @Mock private HistoryService historyService;
    @Mock private ExportService exportService;

    @InjectMocks
    private NodeMainServiceImpl nodeMainService;

    private Update update;
    private Message telegramMessage;
    private ApplicationUser testUser;
    private Task testTask;
    private UserTaskSolution testSolution;

    @BeforeEach
    void setUp() {
        update = mock(Update.class);
        telegramMessage = mock(Message.class);
        when(update.getMessage()).thenReturn(telegramMessage);
        when(telegramMessage.getChatId()).thenReturn(12345L);

        var telegramUser = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        when(telegramMessage.getFrom()).thenReturn(telegramUser);

        testUser = ApplicationUser.builder()
                .id(1L)
                .telegramUserId("12345")
                .userState(UserStateEnum.BASIC_STATE)
                .email("user@example.com")
                .totalPoints(10)
                .build();

        testTask = Task.builder()
                .id(100L)
                .text("Напишите SELECT * FROM books")
                .hintText("Подсказка: используйте SELECT")
                .points(5)
                .build();

        testSolution = UserTaskSolution.builder()
                .id(1L)
                .user(testUser)
                .task(testTask)
                .status(SolutionStatus.PENDING)
                .attempts(0)
                .build();

        when(userService.findOrCreateUser(any())).thenReturn(testUser);
    }

    /**
     * Команда /start, когда email ещё не зарегистрирован → переход в состояние WAIT_FOR_EMAIL.
     */
    @Test
    void processTextMessage_commandStart_withoutEmail_shouldRequestEmail() {
        testUser.setEmail(null);
        testUser.setUserState(UserStateEnum.BASIC_STATE);
        when(telegramMessage.getText()).thenReturn("/start");

        nodeMainService.processTextMassage(update);

        verify(userService).updateUserState(testUser, UserStateEnum.WAIT_FOR_EMAIL_STATE);
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(updateProducer).producerAnswer(captor.capture());
        assertThat(captor.getValue().getText()).contains("Введите ваш email");
        verify(callbackQueryProcessor, never()).sendSectionsMenu(any());
    }

    /**
     * Команда /start, когда email уже есть → показываем главное меню секций.
     */
    @Test
    void processTextMessage_commandStart_withEmail_shouldShowSectionsMenu() {
        testUser.setEmail("user@example.com");
        testUser.setUserState(UserStateEnum.BASIC_STATE);
        when(telegramMessage.getText()).thenReturn("/start");

        nodeMainService.processTextMassage(update);

        verify(userService).clearUserSession(testUser);
        verify(callbackQueryProcessor).sendSectionsMenu("12345");
        verify(updateProducer, never()).producerAnswer(any());
    }

    /**
     * Команда /cancel при наличии активного решения → решение отменяется, состояние сбрасывается.
     */
    @Test
    void processTextMessage_commandCancel_withActivePendingSolution_shouldCancel() {
        when(telegramMessage.getText()).thenReturn("/cancel");
        when(solutionRepository.findTopByUserAndStatusOrderByCreatedAtDesc(testUser, SolutionStatus.PENDING))
                .thenReturn(Optional.of(testSolution));

        nodeMainService.processTextMassage(update);

        assertThat(testSolution.getStatus()).isEqualTo(SolutionStatus.CANCELLED);
        verify(solutionRepository, atLeastOnce()).save(any(UserTaskSolution.class));
        verify(callbackQueryProcessor).sendSectionsMenu("12345");
        verify(updateProducer).producerAnswer(any(SendMessage.class));
    }

    /**
     * Команда /cancel без активного решения – просто сбрасываем состояние и показываем меню.
     */
    @Test
    void processTextMessage_commandCancel_withoutActiveSolution_shouldJustCancel() {
        when(telegramMessage.getText()).thenReturn("/cancel");
        when(solutionRepository.findTopByUserAndStatusOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());

        nodeMainService.processTextMassage(update);

        verify(solutionRepository, never()).save(any());
        verify(userService).updateUserState(testUser, UserStateEnum.BASIC_STATE);
        verify(callbackQueryProcessor).sendSectionsMenu("12345");
    }

    /**
     * Команда /stats – запрашиваем статистику и отправляем сообщение.
     */
    @Test
    void processTextMessage_commandStats_shouldSendStatistics() {
        when(telegramMessage.getText()).thenReturn("/stats");
        UserStatistics stats = UserStatistics.builder()
                .points(50)
                .advancedTotalSolved(2)
                .advancedTotalPoints(40)
                .methodologyLessonStats(List.of())
                .build();
        when(userService.getUserStatistics(testUser)).thenReturn(stats);

        nodeMainService.processTextMassage(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(updateProducer).producerAnswer(captor.capture());
        String text = captor.getValue().getText();
        assertThat(text).contains("Всего баллов: 50");
        assertThat(text).contains("Решено задач: 2");
    }

    /**
     * Команда /export в базовом состоянии → генерируем CSV и отправляем документ.
     */
    @Test
    void processTextMessage_commandExport_whenStateBasic_shouldExport() {
        when(telegramMessage.getText()).thenReturn("/export");
        testUser.setUserState(UserStateEnum.BASIC_STATE);
        byte[] csvData = "test,csv".getBytes();
        when(exportService.generateExportCsv(testUser)).thenReturn(csvData);

        nodeMainService.processTextMassage(update);

        verify(exportService).generateExportCsv(testUser);
        verify(updateProducer).produceDocument(any(byte[].class), anyString(), eq("12345"));
    }

    /**
     * Команда /export в состоянии решения задачи – сообщаем о недоступности.
     */
    @Test
    void processTextMessage_commandExport_whenStateNotBasic_shouldReject() {
        when(telegramMessage.getText()).thenReturn("/export");
        testUser.setUserState(UserStateEnum.WAIT_FOR_TASK_SOLUTION_STATE);

        nodeMainService.processTextMassage(update);

        verify(exportService, never()).generateExportCsv(any());
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(updateProducer).producerAnswer(captor.capture());
        assertThat(captor.getValue().getText()).contains("недоступна во время решения задачи");
    }

    /**
     * Ввод корректного email в состоянии WAIT_FOR_EMAIL → регистрация и показ меню.
     */
    @Test
    void processTextMessage_validEmail_whenWaitingForEmail_shouldRegisterAndShowMenu() {
        String email = "newuser@example.com";
        when(telegramMessage.getText()).thenReturn(email);
        testUser.setUserState(UserStateEnum.WAIT_FOR_EMAIL_STATE);
        when(userService.isEmailValid(email)).thenReturn(true);

        nodeMainService.processTextMassage(update);

        verify(userService).registerEmail(testUser, email);
        verify(callbackQueryProcessor).sendSectionsMenu("12345");
    }

    /**
     * Ввод невалидного email – просим ввести ещё раз.
     */
    @Test
    void processTextMessage_invalidEmail_whenWaitingForEmail_shouldAskAgain() {
        String invalidEmail = "invalid";
        when(telegramMessage.getText()).thenReturn(invalidEmail);
        testUser.setUserState(UserStateEnum.WAIT_FOR_EMAIL_STATE);
        when(userService.isEmailValid(invalidEmail)).thenReturn(false);

        nodeMainService.processTextMassage(update);

        verify(userService, never()).registerEmail(any(), any());
        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(updateProducer).producerAnswer(captor.capture());
        assertThat(captor.getValue().getText()).contains("Некорректный email");
    }

    /**
     * Отправка SQL-решения в состоянии WAIT_FOR_TASK_SOLUTION → отправляем запрос в очередь.
     */
    @Test
    void processTextMessage_solutionSubmission_whenInTaskSolutionState_shouldSendToQueue() {
        String solutionText = "SELECT * FROM books";
        when(telegramMessage.getText()).thenReturn(solutionText);
        testUser.setUserState(UserStateEnum.WAIT_FOR_TASK_SOLUTION_STATE);
        when(solutionRepository.findTopByUserAndStatusOrderByCreatedAtDesc(testUser, SolutionStatus.PENDING))
                .thenReturn(Optional.of(testSolution));

        nodeMainService.processTextMassage(update);

        assertThat(testSolution.getStatus()).isEqualTo(SolutionStatus.PROCESSING);
        verify(solutionRepository).save(testSolution);
        verify(updateProducer).produceSolutionCheck(any(SolutionCheckRequest.class));
    }

    /**
     * Состояние WAIT_FOR_TASK_SOLUTION, но активного решения нет – сбрасываем состояние и показываем меню.
     */
    @Test
    void processTextMessage_solutionSubmission_whenNoActiveSolution_shouldResetState() {
        String solutionText = "SELECT * FROM books";
        when(telegramMessage.getText()).thenReturn(solutionText);
        testUser.setUserState(UserStateEnum.WAIT_FOR_TASK_SOLUTION_STATE);
        when(solutionRepository.findTopByUserAndStatusOrderByCreatedAtDesc(testUser, SolutionStatus.PENDING))
                .thenReturn(Optional.empty());
        when(solutionRepository.findTopByUserAndStatusOrderByCreatedAtDesc(testUser, SolutionStatus.PROCESSING))
                .thenReturn(Optional.empty());

        nodeMainService.processTextMassage(update);

        verify(solutionRepository, never()).save(any());
        verify(userService).updateUserState(testUser, UserStateEnum.BASIC_STATE);
        verify(callbackQueryProcessor).sendSectionsMenu("12345");
        verify(updateProducer, never()).produceSolutionCheck(any());
    }

    /**
     * Отправка решения в тренировочном режиме (WAIT_FOR_TRAINING_SOLUTION) – флаг training=true.
     */
    @Test
    void processTextMessage_trainingSolution_whenInTrainingState_shouldSendToQueue() {
        String solutionText = "SELECT * FROM books";
        when(telegramMessage.getText()).thenReturn(solutionText);
        testUser.setUserState(UserStateEnum.WAIT_FOR_TRAINING_SOLUTION_STATE);
        testUser.setTrainingTaskId(100L);
        when(userService.getTrainingTask(testUser)).thenReturn(100L);
        when(taskService.getTaskById(100L)).thenReturn(testTask);
        when(solutionRepository.existsByUserAndTaskAndStatus(testUser, testTask, SolutionStatus.COMPLETED))
                .thenReturn(true);

        nodeMainService.processTextMassage(update);

        ArgumentCaptor<SolutionCheckRequest> captor = ArgumentCaptor.forClass(SolutionCheckRequest.class);
        verify(updateProducer).produceSolutionCheck(captor.capture());
        assertThat(captor.getValue().isTraining()).isTrue();
        assertThat(captor.getValue().getTaskId()).isEqualTo(100L);
    }
}
