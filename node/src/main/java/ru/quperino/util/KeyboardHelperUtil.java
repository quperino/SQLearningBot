package ru.quperino.util;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.quperino.entities.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилитарный класс для создания inline-клавиатур (кнопок под сообщениями).
 * Содержит статические методы для различных сценариев: меню секций, список занятий,
 * список задач, кнопки навигации и т.д.
 */
public class KeyboardHelperUtil {
    /**
     * Создаёт клавиатуру для выбора секции: "Методичка" и "Повышенный уровень".
     *
     * @return InlineKeyboardMarkup с двумя кнопками
     */
    public static InlineKeyboardMarkup createSectionsKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton methodology = new InlineKeyboardButton();
        methodology.setText("📚 Задачи по методичке");
        methodology.setCallbackData("section_METHODOLOGY");

        InlineKeyboardButton advanced = new InlineKeyboardButton();
        advanced.setText("⭐ Повышенный уровень");
        advanced.setCallbackData("section_ADVANCED");

        rows.add(List.of(methodology));
        rows.add(List.of(advanced));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Создаёт клавиатуру со списком задач (по одной кнопке на задачу).
     *
     * @param tasks              список задач
     * @param withBackToSections добавить ли кнопку "Вернуться к секциям"
     * @return InlineKeyboardMarkup
     */
    public static InlineKeyboardMarkup createTasksKeyboard(List<Task> tasks, boolean withBackToSections) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Task task : tasks) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(task.getTitle());
            button.setCallbackData("task_" + task.getId());
            rows.add(List.of(button));
        }

        if (withBackToSections) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("◀ Вернуться к списку секций");
            backButton.setCallbackData("back_to_sections");
            rows.add(List.of(backButton));
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Клавиатура для тренировочного режима: кнопки "Назад" и "Сбросить решение".
     *
     * @param taskId        ID задачи
     * @param section       секция (METHODOLOGY или ADVANCED)
     * @param lessonNumber  номер занятия (для METHODOLOGY), может быть null
     * @return InlineKeyboardMarkup
     */
    public static InlineKeyboardMarkup createTrainingModeKeyboard(Long taskId, String section, Integer lessonNumber) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀ Вернуться к списку задач");
        String backData = (lessonNumber != null)
                ? "back_to_tasks_" + section + "_" + lessonNumber
                : "back_to_section_" + section;
        backButton.setCallbackData(backData);

        InlineKeyboardButton resetButton = new InlineKeyboardButton();
        resetButton.setText("🔄 Сбросить решение");
        // Добавляем lessonNumber в callback, если он есть
        String resetData = (lessonNumber != null)
                ? "reset_task_" + taskId + "_" + lessonNumber
                : "reset_task_" + taskId;
        resetButton.setCallbackData(resetData);

        rows.add(List.of(backButton));
        rows.add(List.of(resetButton));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Клавиатура для выбора занятия (список занятий секции).
     *
     * @param lessons            список массивов [номер_занятия, название_занятия]
     * @param withBackToSections добавить ли кнопку возврата к секциям
     * @return InlineKeyboardMarkup
     */
    public static InlineKeyboardMarkup createLessonsKeyboard(List<Object[]> lessons, boolean withBackToSections) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Object[] lesson : lessons) {
            Integer lessonNum = (Integer) lesson[0];
            String lessonTitle = (String) lesson[1];
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(lessonTitle);
            button.setCallbackData("lesson_" + lessonNum);
            rows.add(List.of(button));
        }
        if (withBackToSections) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("◀ Вернуться к секциям");
            backButton.setCallbackData("back_to_sections");
            rows.add(List.of(backButton));
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Клавиатура со списком задач конкретного занятия.
     *
     * @param tasks             список задач занятия
     * @param withBackToLessons добавить ли кнопку возврата к списку занятий
     * @return InlineKeyboardMarkup
     */
    public static InlineKeyboardMarkup createTasksForLessonKeyboard(List<Task> tasks, boolean withBackToLessons) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Task task : tasks) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            // Выводим полный title (например, "6.1. INNER JOIN (книги + авторы)")
            button.setText(task.getTitle());
            button.setCallbackData("task_" + task.getId());
            rows.add(List.of(button));
        }
        if (withBackToLessons) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("◀ Вернуться к списку занятий");
            backButton.setCallbackData("back_to_lessons");
            rows.add(List.of(backButton));
        }
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }

    /**
     * Клавиатура для экрана решения задачи: кнопки "Подсказка" и "Назад".
     *
     * @param taskId       ID задачи
     * @param section      секция
     * @param lessonNumber номер занятия (может быть null для ADVANCED)
     * @return InlineKeyboardMarkup
     */
    public static InlineKeyboardMarkup createTaskSolvingKeyboard(Long taskId, String section, Integer lessonNumber) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        InlineKeyboardButton hintButton = new InlineKeyboardButton();
        hintButton.setText("💡 Подсказка");
        hintButton.setCallbackData("hint_" + taskId);

        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("◀ Вернуться к списку задач");
        // Для METHODOLOGY передаём секцию и номер занятия, для ADVANCED – только секцию
        String backData = (lessonNumber != null)
                ? "back_to_tasks_" + section + "_" + lessonNumber
                : "back_to_section_" + section;
        backButton.setCallbackData(backData);

        rows.add(List.of(hintButton));
        rows.add(List.of(backButton));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        return markup;
    }
}
