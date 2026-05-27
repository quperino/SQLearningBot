package ru.quperino.services.impls;

import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.quperino.dto.HistoryEntryDto;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.services.HistoryService;
import ru.quperino.services.UserService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация {@link HistoryService}.
 * Формирует сообщение с историей решений (5 записей на страницу)
 * и добавляет кнопки пагинации "Назад"/"Далее" и закрытия.
 */
@Service
@Log4j2
public class HistoryServiceImpl implements HistoryService {
    private final UserService userService;
    private final NodeUpdateProducerServiceImpl updateProducer;

    public HistoryServiceImpl(UserService userService, NodeUpdateProducerServiceImpl updateProducer) {
        this.userService = userService;
        this.updateProducer = updateProducer;
    }

    @Override
    public void sendHistoryPage(ApplicationUser user, Long chatId, int page) {
        Page<HistoryEntryDto> historyPage = userService.getUserHistory(user, page, 5);
        if (historyPage.isEmpty()) {
            SendMessage msg = new SendMessage(chatId.toString(), "У вас пока нет решённых задач.");
            updateProducer.producerAnswer(msg);
            return;
        }

        StringBuilder sb = new StringBuilder("📋 **История решений**\n\n");
        for (HistoryEntryDto entry : historyPage.getContent()) {
            sb.append(String.format("*%s* - %s\n", entry.getTaskTitle(), entry.getStatus()))
                    .append(String.format("   Секция: %s\n", entry.getSection()))
                    .append(String.format("   Дата: %s\n", formatDateTime(entry.getDate())))
                    .append(String.format("   Попыток: %d\n\n", entry.getAttempts()));
        }

        SendMessage message = new SendMessage(chatId.toString(), sb.toString());
        message.enableMarkdown(true);

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        if (page > 0) {
            InlineKeyboardButton prev = new InlineKeyboardButton();
            prev.setText("◀ Назад");
            prev.setCallbackData("history_page_" + (page - 1));
            row.add(prev);
        }
        if (historyPage.hasNext()) {
            InlineKeyboardButton next = new InlineKeyboardButton();
            next.setText("Далее ▶");
            next.setCallbackData("history_page_" + (page + 1));
            row.add(next);
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }

        InlineKeyboardButton close = new InlineKeyboardButton();
        close.setText("❌ Закрыть");
        close.setCallbackData("history_close");
        rows.add(List.of(close));

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);
        updateProducer.producerAnswer(message);
    }

    /**
     * Форматирует дату и время в локализованную строку (дд.ММ.гггг чч:мм).
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "—";
        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}
