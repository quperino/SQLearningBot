package ru.quperino.services.impls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.quperino.entities.Task;
import ru.quperino.repositories.TaskRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тест {@link TaskServiceImpl}. Проверяет получение задач по ID и по секции.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {
    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task testTask;

    @BeforeEach
    void setUp() {
        testTask = Task.builder()
                .id(1L)
                .section("METHODOLOGY")
                .title("1.1. Тестовая задача")
                .text("Условие задачи")
                .points(5)
                .build();
    }

    /**
     * Проверяет, что {@code getTaskById} возвращает задачу, если она существует в БД.
     */
    @Test
    void getTaskById_whenExists_shouldReturnTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        Task result = taskService.getTaskById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("1.1. Тестовая задача");
        verify(taskRepository).findById(1L);
    }

    /**
     * Проверяет, что {@code getTaskById} возвращает null, если задача не найдена.
     */
    @Test
    void getTaskById_whenNotExists_shouldReturnNull() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        Task result = taskService.getTaskById(999L);

        assertThat(result).isNull();
        verify(taskRepository).findById(999L);
    }

    /**
     * Проверяет, что {@code getTasksBySection} возвращает список задач заданной секции.
     */
    @Test
    void getTasksBySection_shouldReturnListOfTasks() {
        Task anotherTask = Task.builder()
                .id(2L)
                .section("METHODOLOGY")
                .title("1.2. Другая задача")
                .build();
        List<Task> expected = List.of(testTask, anotherTask);
        when(taskRepository.findBySectionOrderById("METHODOLOGY")).thenReturn(expected);

        List<Task> result = taskService.getTasksBySection("METHODOLOGY");

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(testTask, anotherTask);
        verify(taskRepository).findBySectionOrderById("METHODOLOGY");
    }

    /**
     * Проверяет, что {@code getTasksBySection} возвращает пустой список, если задач нет.
     */
    @Test
    void getTasksBySection_whenNoTasks_shouldReturnEmptyList() {
        when(taskRepository.findBySectionOrderById("ADVANCED")).thenReturn(List.of());

        List<Task> result = taskService.getTasksBySection("ADVANCED");

        assertThat(result).isEmpty();
        verify(taskRepository).findBySectionOrderById("ADVANCED");
    }
}
