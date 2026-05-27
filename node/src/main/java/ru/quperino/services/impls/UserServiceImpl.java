package ru.quperino.services.impls;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.quperino.dto.HistoryEntryDto;
import ru.quperino.dto.UserStatistics;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.PointsHistory;
import ru.quperino.entities.Task;
import ru.quperino.entities.UserTaskSolution;
import ru.quperino.entities.enums.SolutionStatus;
import ru.quperino.entities.enums.UserStateEnum;
import ru.quperino.repositories.*;
import ru.quperino.services.UserService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Реализация {@link UserService}.
 * Содержит бизнес-логику для управления пользователями: создание, обновление состояния,
 * регистрация email, начисление/списание баллов, сброс данных, статистика.
 */
@Service
@Log4j2
public class UserServiceImpl implements UserService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final ApplicationUserRepository userRepository;
    private final UserTaskSolutionRepository solutionRepository;
    private final UserMessageRepository userMessageRepository;
    private final BotMessageRepository botMessageRepository;
    private final ButtonClickRepository buttonClickRepository;
    private final TaskRepository taskRepository;
    private final PointsHistoryRepository pointsHistoryRepository;

    @Autowired
    public UserServiceImpl(ApplicationUserRepository userRepository,
                           UserTaskSolutionRepository solutionRepository,
                           UserMessageRepository userMessageRepository,
                           BotMessageRepository botMessageRepository,
                           ButtonClickRepository buttonClickRepository,
                           TaskRepository taskRepository,
                           PointsHistoryRepository pointsHistoryRepository) {
        this.userRepository = userRepository;
        this.solutionRepository = solutionRepository;
        this.userMessageRepository = userMessageRepository;
        this.botMessageRepository = botMessageRepository;
        this.buttonClickRepository = buttonClickRepository;
        this.taskRepository = taskRepository;
        this.pointsHistoryRepository = pointsHistoryRepository;
    }

    @Override
    public ApplicationUser findOrCreateUser(User telegramUser) {
        ApplicationUser user = userRepository.findApplicationUserByTelegramUserId(telegramUser.getId().toString());
        if (user == null) {
            // Новый пользователь – создаём запись
            user = ApplicationUser.builder()
                    .telegramUserId(telegramUser.getId().toString())
                    .username(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    .isActive(true)
                    .userState(UserStateEnum.BASIC_STATE)
                    .build();
            user = userRepository.save(user);
        } else if (user.getUserState() == null) {
            // На случай старых записей без состояния – восстанавливаем
            user.setUserState(UserStateEnum.BASIC_STATE);
            user = userRepository.save(user);
        }
        return user;
    }

    @Override
    public ApplicationUser updateUserState(ApplicationUser user, UserStateEnum state) {
        user.setUserState(state);
        return userRepository.save(user);
    }

    @Override
    public boolean isEmailValid(String email) {
        if (email == null || email.isBlank()) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    @Override
    public ApplicationUser registerEmail(ApplicationUser user, String email) {
        user.setEmail(email);
        user.setUserState(UserStateEnum.BASIC_STATE);
        log.debug("[NODE] Пользователь {} зарегистрировал email: {}", user.getId(), maskEmail(email));
        return userRepository.save(user);
    }

    /**
     * Маскирует email для логирования (показывает только первый символ и домен).
     *
     * @param email исходный email
     * @return замаскированная строка
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        if (local.length() <= 2) return "***@" + parts[1];
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + parts[1];
    }

    @Override
    public ApplicationUser findOrCreateUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Override
    public void clearUserSession(ApplicationUser user) {
        log.debug("[NODE] Сброс сессии пользователя {}", user.getId());
        // Отменяем все PENDING и PROCESSING решения
        List<UserTaskSolution> pending = solutionRepository.findByUserAndStatus(user, SolutionStatus.PENDING);
        List<UserTaskSolution> processing = solutionRepository.findByUserAndStatus(user, SolutionStatus.PROCESSING);
        List<UserTaskSolution> allActive = new ArrayList<>();
        allActive.addAll(pending);
        allActive.addAll(processing);
        for (UserTaskSolution s : allActive) {
            s.setStatus(SolutionStatus.CANCELLED);
            solutionRepository.save(s);
        }

        // Сбрасываем состояние пользователя
        user.setUserState(UserStateEnum.BASIC_STATE);
        userRepository.save(user);
    }

    @Transactional
    @Override
    public void resetUserData(ApplicationUser user) {
        log.debug("[NODE] Полный сброс данных пользователя {}", user.getId());
        solutionRepository.deleteAll(solutionRepository.findByUser(user));
        userMessageRepository.deleteAll(userMessageRepository.findByUser(user));
        botMessageRepository.deleteAll(botMessageRepository.findByUser(user));
        buttonClickRepository.deleteAll(buttonClickRepository.findByUser(user));
        user.setEmail(null);
        user.setTotalPoints(0);
        user.setTrainingTaskId(null);
        user.setUserState(UserStateEnum.BASIC_STATE);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void addPoints(ApplicationUser user, int points) {
        if (user == null || points <= 0) return;
        int current = user.getTotalPoints() != null ? user.getTotalPoints() : 0;
        user.setTotalPoints(current + points);
        userRepository.save(user);
        log.debug("[NODE] Пользователю {} начислено {} баллов, всего: {}", user.getId(), points, current + points);
    }

    @Override
    public UserStatistics getUserStatistics(ApplicationUser user) {
        if (user == null) return UserStatistics.builder().build();

        // Все завершённые решения
        List<UserTaskSolution> completedSolutions = solutionRepository.findByUserAndStatus(user, SolutionStatus.COMPLETED);

        // Статистика по занятиям методички: lessonNumber -> [solvedTasks, earnedPoints]
        Map<Integer, int[]> lessonStatsMap = new HashMap<>();

        int advancedSolved = 0;
        int advancedPoints = 0;

        for (UserTaskSolution sol : completedSolutions) {
            Task task = sol.getTask();
            if ("METHODOLOGY".equals(task.getSection())) {
                Integer lessonNum = task.getLessonNumber();
                if (lessonNum == null) lessonNum = 0;
                int[] stats = lessonStatsMap.computeIfAbsent(lessonNum, k -> new int[]{0, 0});
                stats[0]++; // solvedTasks
                stats[1] += task.getPoints(); // earnedPoints
            } else if ("ADVANCED".equals(task.getSection())) {
                advancedSolved++;
                advancedPoints += task.getPoints();
            }
        }

        // Формируем список занятий с названиями
        List<UserStatistics.LessonStats> lessonStats = new ArrayList<>();
        for (Map.Entry<Integer, int[]> entry : lessonStatsMap.entrySet()) {
            Integer lessonNum = entry.getKey();
            int[] stats = entry.getValue();
            String lessonTitle; // нужно получить из любой задачи этого занятия
            // Находим название занятия по любой задаче из этого занятия
            List<Task> anyTask = taskRepository.findBySectionAndLessonNumberOrderByTaskNumberAsc("METHODOLOGY", lessonNum);
            if (!anyTask.isEmpty()) {
                lessonTitle = anyTask.get(0).getLessonTitle();
            } else {
                lessonTitle = "Занятие " + lessonNum;
            }
            lessonStats.add(UserStatistics.LessonStats.builder()
                    .lessonNumber(lessonNum)
                    .lessonTitle(lessonTitle)
                    .solvedTasks(stats[0])
                    .earnedPoints(stats[1])
                    .build());
        }

        lessonStats.sort(Comparator.comparingInt(UserStatistics.LessonStats::getLessonNumber));

        int totalPoints = user.getTotalPoints() != null ? user.getTotalPoints() : 0;

        return UserStatistics.builder()
                .points(totalPoints)
                .methodologyLessonStats(lessonStats)
                .advancedTotalSolved(advancedSolved)
                .advancedTotalPoints(advancedPoints)
                .build();
    }

    @Override
    public void setTrainingTask(ApplicationUser user, Long taskId) {
        user.setTrainingTaskId(taskId);
        userRepository.save(user);
    }

    @Override
    public Long getTrainingTask(ApplicationUser user) {
        return user.getTrainingTaskId();
    }

    @Override
    public void clearTrainingTask(ApplicationUser user) {
        user.setTrainingTaskId(null);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resetTaskProgress(ApplicationUser user, Long taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.warn("[NODE] Попытка сброса прогресса для несуществующей задачи id={}", taskId);
            return;
        }

        // Ищем завершённое решение по этой задаче
        Optional<UserTaskSolution> solutionOpt = solutionRepository.findFirstByUserAndTaskAndStatusOrderByCompletedAtDesc(user, task, SolutionStatus.COMPLETED);
        if (solutionOpt.isPresent()) {
            UserTaskSolution solution = solutionOpt.get();
            if (solution.getStatus() == SolutionStatus.COMPLETED) {
                solution.setStatus(SolutionStatus.PENDING);
                solution.setAttempts(0);
                solution.setLastCorrectSolution(null);
                solution.setCompletedAt(null);
                solution.setAiFeedback(null);
                solutionRepository.save(solution);
                subtractPoints(user, task.getPoints(), task);
                log.debug("[NODE] Сброшен прогресс по задаче {} для пользователя {}", taskId, user.getId());
            } else {
                log.debug("[NODE] Задача {} не была решена, сброс не требуется", taskId);
            }
        } else {
            log.debug("[NODE] Запись решения для задачи {} у пользователя {} не найдена", taskId, user.getId());
        }
        clearTrainingTask(user);
        if (user.getUserState() == UserStateEnum.WAIT_FOR_TRAINING_SOLUTION_STATE) {
            updateUserState(user, UserStateEnum.BASIC_STATE);
        }
    }

    @Override
    @Transactional
    public void subtractPoints(ApplicationUser user, int points, Task task) {
        if (user == null || points <= 0) return;
        int current = user.getTotalPoints() != null ? user.getTotalPoints() : 0;
        user.setTotalPoints(current - points);
        userRepository.save(user);

        PointsHistory history = PointsHistory.builder()
                .user(user)
                .task(task)
                .points(-points)
                .createdAt(LocalDateTime.now())
                .build();
        pointsHistoryRepository.save(history);

        log.debug("[NODE] У пользователя {} вычтено {} баллов за сброс задачи {}, всего: {}",
                user.getId(), points, task.getId(), current - points);
    }

    @Override
    public Page<HistoryEntryDto> getUserHistory(ApplicationUser user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "completedAt"));
        Page<UserTaskSolution> solutionPage = solutionRepository.findByUserOrderByCompletedAtDesc(user, pageable);
        return solutionPage.map(solution -> {
            String statusText;
            if (solution.getStatus() == SolutionStatus.COMPLETED) {
                statusText = "✅ Решено";
            } else if (solution.getStatus() == SolutionStatus.FAILED) {
                statusText = "❌ Неверно";
            } else {
                statusText = "⏳ Не завершено";
            }
            return HistoryEntryDto.builder()
                    .section(solution.getTask().getSection())
                    .taskTitle(solution.getTask().getTitle())
                    .status(statusText)
                    .date(solution.getCompletedAt() != null ? solution.getCompletedAt() : solution.getCreatedAt())
                    .attempts(solution.getAttempts())
                    .build();
        });
    }
}
