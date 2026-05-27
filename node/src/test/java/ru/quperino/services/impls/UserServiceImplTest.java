package ru.quperino.services.impls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.quperino.dto.HistoryEntryDto;
import ru.quperino.dto.UserStatistics;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.PointsHistory;
import ru.quperino.entities.Task;
import ru.quperino.entities.UserTaskSolution;
import ru.quperino.entities.enums.SolutionStatus;
import ru.quperino.entities.enums.UserStateEnum;
import ru.quperino.repositories.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты для {@link UserServiceImpl} – проверка бизнес-логики
 * управления пользователями.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private ApplicationUserRepository userRepository;
    @Mock private UserTaskSolutionRepository solutionRepository;
    @Mock private UserMessageRepository userMessageRepository;
    @Mock private BotMessageRepository botMessageRepository;
    @Mock private ButtonClickRepository buttonClickRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private PointsHistoryRepository pointsHistoryRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private ApplicationUser testUser;

    @BeforeEach
    void setUp() {
        testUser = ApplicationUser.builder()
                .id(1L)
                .telegramUserId("12345")
                .userState(UserStateEnum.BASIC_STATE)
                .totalPoints(0)
                .build();
    }

    // ----- findOrCreateUser -----
    @Test
    void findOrCreateUser_whenUserExists_shouldReturnExisting() {
        var telegramUser = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        when(telegramUser.getId()).thenReturn(12345L);
        when(userRepository.findApplicationUserByTelegramUserId("12345")).thenReturn(testUser);

        ApplicationUser result = userService.findOrCreateUser(telegramUser);

        assertThat(result).isSameAs(testUser);
        verify(userRepository, never()).save(any());
    }

    @Test
    void findOrCreateUser_whenUserDoesNotExist_shouldCreateNewUser() {
        var telegramUser = mock(org.telegram.telegrambots.meta.api.objects.User.class);
        when(telegramUser.getId()).thenReturn(12345L);
        when(telegramUser.getUserName()).thenReturn("testUser");
        when(telegramUser.getFirstName()).thenReturn("Test");
        when(telegramUser.getLastName()).thenReturn("User");
        when(userRepository.findApplicationUserByTelegramUserId("12345")).thenReturn(null);
        when(userRepository.save(any(ApplicationUser.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationUser result = userService.findOrCreateUser(telegramUser);

        assertThat(result).isNotNull();
        assertThat(result.getTelegramUserId()).isEqualTo("12345");
        assertThat(result.getUsername()).isEqualTo("testUser");
        assertThat(result.getUserState()).isEqualTo(UserStateEnum.BASIC_STATE);
        verify(userRepository).save(any(ApplicationUser.class));
    }

    // ----- registerEmail -----
    @Test
    void registerEmail_shouldSetEmailAndState() {
        String email = "user@example.com";
        when(userRepository.save(any(ApplicationUser.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationUser result = userService.registerEmail(testUser, email);

        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getUserState()).isEqualTo(UserStateEnum.BASIC_STATE);
        verify(userRepository).save(testUser);
    }

    // ----- isEmailValid -----
    @Test
    void isEmailValid_shouldReturnTrueForValidEmail() {
        assertThat(userService.isEmailValid("user@example.com")).isTrue();
        assertThat(userService.isEmailValid("name.surname@domain.co.uk")).isTrue();
    }

    @Test
    void isEmailValid_shouldReturnFalseForInvalidEmail() {
        assertThat(userService.isEmailValid("")).isFalse();
        assertThat(userService.isEmailValid(null)).isFalse();
    }

    // ----- addPoints -----
    @Test
    void addPoints_shouldIncreaseTotalPoints() {
        testUser.setTotalPoints(10);
        when(userRepository.save(any(ApplicationUser.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.addPoints(testUser, 5);

        assertThat(testUser.getTotalPoints()).isEqualTo(15);
        verify(userRepository).save(testUser);
    }

    @Test
    void addPoints_shouldDoNothingWhenPointsZeroOrNegative() {
        testUser.setTotalPoints(10);
        userService.addPoints(testUser, 0);
        userService.addPoints(testUser, -5);
        assertThat(testUser.getTotalPoints()).isEqualTo(10);
        verify(userRepository, never()).save(any());
    }

    // ----- resetTaskProgress -----
    @Test
    void resetTaskProgress_shouldResetCompletedSolutionAndSubtractPoints() {
        Task task = Task.builder().id(10L).points(20).build();
        UserTaskSolution solution = UserTaskSolution.builder()
                .status(SolutionStatus.COMPLETED)
                .attempts(3)
                .lastCorrectSolution("SELECT * FROM books")
                .build();
        testUser.setTotalPoints(50);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(solutionRepository.findFirstByUserAndTaskAndStatusOrderByCompletedAtDesc(
                testUser, task, SolutionStatus.COMPLETED))
                .thenReturn(Optional.of(solution));
        when(solutionRepository.save(any(UserTaskSolution.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(ApplicationUser.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.resetTaskProgress(testUser, 10L);

        assertThat(solution.getStatus()).isEqualTo(SolutionStatus.PENDING);
        assertThat(solution.getAttempts()).isZero();
        assertThat(solution.getLastCorrectSolution()).isNull();
        assertThat(testUser.getTotalPoints()).isEqualTo(30);

        verify(solutionRepository).save(solution);
        verify(pointsHistoryRepository).save(any(PointsHistory.class));
    }

    @Test
    void resetTaskProgress_shouldDoNothingIfTaskNotCompleted() {
        Task task = Task.builder().id(10L).points(20).build();
        testUser.setTotalPoints(50);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(solutionRepository.findFirstByUserAndTaskAndStatusOrderByCompletedAtDesc(
                testUser, task, SolutionStatus.COMPLETED))
                .thenReturn(Optional.empty());

        userService.resetTaskProgress(testUser, 10L);

        assertThat(testUser.getTotalPoints()).isEqualTo(50);
        verify(solutionRepository, never()).save(any());
    }

    // ----- getUserStatistics -----
    @Test
    void getUserStatistics_shouldReturnCorrectStats() {
        Task task1 = Task.builder().id(1L).section("METHODOLOGY").lessonNumber(1).lessonTitle("Занятие 1").points(10).build();
        Task task2 = Task.builder().id(2L).section("METHODOLOGY").lessonNumber(1).lessonTitle("Занятие 1").points(10).build();
        Task taskAdvanced = Task.builder().id(3L).section("ADVANCED").points(20).build();

        UserTaskSolution sol1 = UserTaskSolution.builder().task(task1).status(SolutionStatus.COMPLETED).build();
        UserTaskSolution sol2 = UserTaskSolution.builder().task(task2).status(SolutionStatus.COMPLETED).build();
        UserTaskSolution solAdv = UserTaskSolution.builder().task(taskAdvanced).status(SolutionStatus.COMPLETED).build();

        when(solutionRepository.findByUserAndStatus(testUser, SolutionStatus.COMPLETED))
                .thenReturn(List.of(sol1, sol2, solAdv));
        when(taskRepository.findBySectionAndLessonNumberOrderByTaskNumberAsc("METHODOLOGY", 1))
                .thenReturn(List.of(task1));

        testUser.setTotalPoints(40);

        UserStatistics stats = userService.getUserStatistics(testUser);

        assertThat(stats.getPoints()).isEqualTo(40);
        assertThat(stats.getAdvancedTotalSolved()).isEqualTo(1);
        assertThat(stats.getAdvancedTotalPoints()).isEqualTo(20);
        assertThat(stats.getMethodologyLessonStats()).hasSize(1);
        assertThat(stats.getMethodologyLessonStats().get(0).getSolvedTasks()).isEqualTo(2);
        assertThat(stats.getMethodologyLessonStats().get(0).getEarnedPoints()).isEqualTo(20);
    }

    // ----- getUserHistory -----
    @Test
    void getUserHistory_shouldReturnPagedHistory() {
        Task task = Task.builder().id(1L).section("METHODOLOGY").title("1.1. Тест").build();
        UserTaskSolution solution = UserTaskSolution.builder()
                .task(task)
                .status(SolutionStatus.COMPLETED)
                .attempts(2)
                .completedAt(LocalDateTime.now())
                .build();
        Page<UserTaskSolution> page = new PageImpl<>(List.of(solution));
        when(solutionRepository.findByUserOrderByCompletedAtDesc(eq(testUser), any(Pageable.class)))
                .thenReturn(page);

        Page<HistoryEntryDto> result = userService.getUserHistory(testUser, 0, 5);

        assertThat(result.getContent()).hasSize(1);
        HistoryEntryDto dto = result.getContent().get(0);
        assertThat(dto.getTaskTitle()).isEqualTo("1.1. Тест");
        assertThat(dto.getAttempts()).isEqualTo(2);
        assertThat(dto.getStatus()).isEqualTo("✅ Решено");
    }
}
