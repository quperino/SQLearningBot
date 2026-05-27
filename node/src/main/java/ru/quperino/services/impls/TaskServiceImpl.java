package ru.quperino.services.impls;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.quperino.entities.Task;
import ru.quperino.repositories.TaskRepository;
import ru.quperino.services.TaskService;

import java.util.List;

/**
 * Реализация {@link TaskService}.
 * Сервис для делегирования репозиторию.
 */
@Service
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;

    @Autowired
    public TaskServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public Task getTaskById(Long id) {
        return taskRepository.findById(id).orElse(null);
    }

    @Override
    public List<Task> getTasksBySection(String section) {
        return taskRepository.findBySectionOrderById(section);
    }
}
