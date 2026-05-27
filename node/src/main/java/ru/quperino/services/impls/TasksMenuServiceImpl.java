package ru.quperino.services.impls;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.quperino.entities.Task;
import ru.quperino.repositories.TaskRepository;
import ru.quperino.services.TaskService;
import ru.quperino.services.TasksMenuService;
import ru.quperino.util.KeyboardHelperUtil;

import java.util.List;

/**
 * Реализация {@link TasksMenuService}.
 * Формирует и отправляет inline-клавиатуры для выбора занятий и задач.
 */
@Service
@Log4j2
public class TasksMenuServiceImpl implements TasksMenuService {
    private final NodeUpdateProducerServiceImpl updateProducer;
    private final TaskService taskService;
    private final TaskRepository taskRepository;

    @Autowired
    public TasksMenuServiceImpl(NodeUpdateProducerServiceImpl updateProducer,
                                TaskService taskService,
                                TaskRepository taskRepository) {
        this.updateProducer = updateProducer;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
    }

    @Override
    public void sendLessonsBySection(Long chatId, String section) {
        List<Object[]> lessons = taskRepository.findDistinctLessonsBySection(section);
        if (lessons.isEmpty()) {
            SendMessage msg = new SendMessage(String.valueOf(chatId), "В этой секции пока нет занятий.");
            updateProducer.producerAnswer(msg);
            return;
        }
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Выберите занятие:");
        sendMessage.setReplyMarkup(KeyboardHelperUtil.createLessonsKeyboard(lessons, true));
        updateProducer.producerAnswer(sendMessage);
    }

    @Override
    public void sendTasksByLesson(Long chatId, String section, Integer lessonNumber) {
        List<Task> tasks = taskRepository.findBySectionAndLessonNumberOrderByTaskNumberAsc(section, lessonNumber);
        if (tasks.isEmpty()) {
            SendMessage msg = new SendMessage(String.valueOf(chatId), "В этом занятии пока нет задач.");
            updateProducer.producerAnswer(msg);
            return;
        }
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Задачи занятия:");
        sendMessage.setReplyMarkup(KeyboardHelperUtil.createTasksForLessonKeyboard(tasks, true));
        updateProducer.producerAnswer(sendMessage);
    }

    @Override
    public void sendTasksBySection(Long chatId, String section) {
        List<Task> tasks = taskService.getTasksBySection(section);
        if (tasks.isEmpty()) {
            SendMessage msg = new SendMessage(String.valueOf(chatId), "В этой секции пока нет задач.");
            updateProducer.producerAnswer(msg);
            return;
        }
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Выберите задачу из секции " + ("METHODOLOGY".equals(section) ? "Методичка" : "Повышенный уровень") + ":");
        sendMessage.setReplyMarkup(KeyboardHelperUtil.createTasksKeyboard(tasks, true));
        updateProducer.producerAnswer(sendMessage);
    }
}
