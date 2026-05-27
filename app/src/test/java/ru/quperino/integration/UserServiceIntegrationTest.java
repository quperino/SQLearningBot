package ru.quperino.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.quperino.dto.UserStatistics;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.Task;
import ru.quperino.entities.UserTaskSolution;
import ru.quperino.entities.enums.SolutionStatus;
import ru.quperino.repositories.TaskRepository;
import ru.quperino.repositories.UserTaskSolutionRepository;
import ru.quperino.services.UserService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционные тесты для {@link UserService}.
 * Проверяют создание пользователя, регистрацию email, начисление баллов и получение статистики.
 */
public class UserServiceIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private UserService userService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserTaskSolutionRepository solutionRepository;

    /**
     * Тест создания пользователя и регистрации email.
     *   Создаём фиктивный Telegram-объект User.
     *   Вызываем {@link UserService#findOrCreateUser(User)} – должен создать нового пользователя.
     *   Проверяем корректность telegramUserId и начального состояния.
     *   Регистрируем email через {@link UserService#registerEmail(ApplicationUser, String)}.
     *   Проверяем, что email сохранился.
     */
    @Test
    void createUserAndRegisterEmail() {
        // given
        var telegramUser = new org.telegram.telegrambots.meta.api.objects.User();
        telegramUser.setId(12345L);
        telegramUser.setFirstName("Test");
        telegramUser.setLastName("User");

        // when
        ApplicationUser user = userService.findOrCreateUser(telegramUser);

        // then
        assertThat(user.getTelegramUserId()).isEqualTo("12345");
        assertThat(user.getUserState()).isNotNull();

        // when
        user = userService.registerEmail(user, "test@example.com");

        // then
        assertThat(user.getEmail()).isEqualTo("test@example.com");
    }

    /**
     * Тест начисления баллов и получения статистики.
     *   Создаём пользователя.
     *   Находим задачу из data.sql (предполагается, что существует задача с id=1).
     *   Начисляем баллы за задачу через {@link UserService#addPoints(ApplicationUser, int)}.
     *   Создаём решение со статусом COMPLETED, имитируя успешное выполнение.
     *   Вызываем {@link UserService#getUserStatistics(ApplicationUser)} и проверяем,
     *       что баллы совпадают с баллами задачи, и статистика по занятиям не пуста.
     */
    @Test
    void addPointsAndGetStatistics() {
        // given
        var telegramUser = new User();
        telegramUser.setId(12345L);
        ApplicationUser user = userService.findOrCreateUser(telegramUser);

        // Находим задачу из data.sql (предполагаем, что есть задача с id=1)
        Task task = taskRepository.findById(1L).orElseThrow();
        userService.addPoints(user, task.getPoints());

        // Создаём решение (имитируем, что задача решена)
        UserTaskSolution solution = UserTaskSolution.builder()
                .user(user)
                .task(task)
                .status(SolutionStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .build();
        solutionRepository.save(solution);

        // when
        UserStatistics stats = userService.getUserStatistics(user);

        // then
        assertThat(stats.getPoints()).isEqualTo(task.getPoints());
        assertThat(stats.getMethodologyLessonStats()).isNotEmpty();
    }
}
