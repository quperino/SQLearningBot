package ru.quperino.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.UserTaskSolution;
import ru.quperino.entities.enums.SolutionStatus;
import ru.quperino.entities.enums.UserStateEnum;
import ru.quperino.repositories.ApplicationUserRepository;
import ru.quperino.repositories.TaskRepository;
import ru.quperino.repositories.UserTaskSolutionRepository;
import ru.quperino.services.NodeMainService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест, моделирующий полный путь пользователя:
 *   Отправка команды /start и регистрация email.
 *   Выбор задачи и отправка SQL-решения.
 *   Проверка, что решение переведено в статус PROCESSING и отправлено на проверку.
 */
public class NodeMainServiceIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private NodeMainService nodeMainService;

    @Autowired
    private ApplicationUserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserTaskSolutionRepository solutionRepository;

    private Update update;
    private Message message;

    /**
     * Подготавливает фиктивное сообщение перед каждым тестом.
     * Создаёт объекты Update, Message, Chat и User с одинаковым ID (99999L).
     */
    @BeforeEach
    void setUp() {
        update = new Update();
        message = new Message();
        User telegramUser = new User();
        telegramUser.setId(99999L);
        message.setFrom(telegramUser);

        Chat chat = new Chat();
        chat.setId(99999L);
        message.setChat(chat);

        update.setMessage(message);
    }

    /**
     * Полный сценарий: регистрация → выбор задачи → отправка решения.
     */
    @Test
    void testFullUserJourney() {
        // 1. Пользователь отправляет /start
        message.setText("/start");
        nodeMainService.processTextMassage(update);

        ApplicationUser user = userRepository.findApplicationUserByTelegramUserId("99999");
        assertThat(user).isNotNull();
        assertThat(user.getUserState()).isEqualTo(UserStateEnum.WAIT_FOR_EMAIL_STATE);

        // 2. Пользователь вводит email
        message.setText("test@example.com");
        nodeMainService.processTextMassage(update);

        user = userRepository.findById(user.getId()).orElseThrow();
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getUserState()).isEqualTo(UserStateEnum.BASIC_STATE);

        // 3. Симулируем выбор задачи: создаём PENDING решение
        var task = taskRepository.findBySectionOrderById("METHODOLOGY").get(0);
        UserTaskSolution solution = UserTaskSolution.builder()
                .user(user)
                .task(task)
                .status(SolutionStatus.PENDING)
                .attempts(0)
                .build();
        solution = solutionRepository.save(solution);

        // Переводим пользователя в состояние ожидания решения
        user.setUserState(UserStateEnum.WAIT_FOR_TASK_SOLUTION_STATE);
        userRepository.save(user);

        // 4. Пользователь отправляет решение
        String userSql = "SELECT * FROM books";
        message.setText(userSql);

        nodeMainService.processTextMassage(update);

        // 5. Проверяем, что решение переведено в PROCESSING (запрос на проверку отправлен)
        UserTaskSolution finalSolution = solutionRepository.findById(solution.getId()).orElseThrow();
        assertThat(finalSolution).isNotNull();
        assertThat(finalSolution.getStatus()).isEqualTo(SolutionStatus.PROCESSING);
        assertThat(finalSolution.getLastCorrectSolution()).isNull();
        assertThat(finalSolution.getAttempts()).isZero();
    }
}
