package ru.quperino.services.impls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.quperino.dto.HistoryEntryDto;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.services.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты {@link HistoryServiceImpl} – проверка построения сообщений
 * с историей решений и кнопок пагинации.
 */
@ExtendWith(MockitoExtension.class)
class HistoryServiceImplTest {

    private final Long chatId = 12345L;

    @Mock private UserService userService;
    @Mock private NodeUpdateProducerServiceImpl updateProducer;

    @InjectMocks
    private HistoryServiceImpl historyService;

    private ApplicationUser testUser;

    @BeforeEach
    void setUp() {
        testUser = ApplicationUser.builder().id(1L).build();
    }

    /**
     * Если история пуста – отправляется короткое сообщение без клавиатуры.
     */
    @Test
    void sendHistoryPage_whenEmpty_shouldSendEmptyMessage() {
        Page<HistoryEntryDto> emptyPage = new PageImpl<>(List.of());
        when(userService.getUserHistory(eq(testUser), eq(0), eq(5))).thenReturn(emptyPage);

        historyService.sendHistoryPage(testUser, chatId, 0);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(updateProducer).producerAnswer(captor.capture());
        SendMessage sent = captor.getValue();
        assertThat(sent.getText()).isEqualTo("У вас пока нет решённых задач.");
        assertThat(sent.getReplyMarkup()).isNull();
    }

    /**
     * Первая страница, есть следующая → должна быть кнопка "Далее".
     */
    @Test
    void sendHistoryPage_withFirstPageAndNextPage_shouldContainNextButtonOnly() {
        HistoryEntryDto entry = HistoryEntryDto.builder()
                .section("METHODOLOGY")
                .taskTitle("1.1. Создание таблицы")
                .status("✅ Решено")
                .date(LocalDateTime.of(2025, 3, 15, 10, 30))
                .attempts(2)
                .build();
        int pageNumber = 0;
        int pageSize = 5;
        long totalElements = 10;
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        Page<HistoryEntryDto> page = new PageImpl<>(List.of(entry), pageable, totalElements);
        when(userService.getUserHistory(testUser, pageNumber, pageSize)).thenReturn(page);

        historyService.sendHistoryPage(testUser, chatId, pageNumber);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(updateProducer).producerAnswer(captor.capture());
        SendMessage sent = captor.getValue();

        assertThat(sent.getText()).contains("📋 **История решений**");
        assertThat(sent.getText()).contains("1.1. Создание таблицы");

        InlineKeyboardMarkup keyboard = (InlineKeyboardMarkup) sent.getReplyMarkup();
        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        assertThat(rows).hasSize(2);
        List<InlineKeyboardButton> navRow = rows.get(0);
        assertThat(navRow).hasSize(1);
        assertThat(navRow.get(0).getText()).isEqualTo("Далее ▶");
        assertThat(navRow.get(0).getCallbackData()).isEqualTo("history_page_1");
    }

    /**
     * Серединная страница → кнопки "Назад" и "Далее".
     */
    @Test
    void sendHistoryPage_withMiddlePage_shouldContainBothPrevAndNextButtons() {
        HistoryEntryDto entry = HistoryEntryDto.builder()
                .taskTitle("2.1. JOIN")
                .status("❌ Неверно")
                .date(LocalDateTime.now())
                .attempts(1)
                .build();
        int pageNumber = 1;
        int pageSize = 5;
        long totalElements = 20;
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        Page<HistoryEntryDto> page = new PageImpl<>(List.of(entry), pageable, totalElements);
        when(userService.getUserHistory(testUser, pageNumber, pageSize)).thenReturn(page);

        historyService.sendHistoryPage(testUser, chatId, pageNumber);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(updateProducer).producerAnswer(captor.capture());
        SendMessage sent = captor.getValue();

        InlineKeyboardMarkup keyboard = (InlineKeyboardMarkup) sent.getReplyMarkup();
        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        List<InlineKeyboardButton> navRow = rows.get(0);
        assertThat(navRow).hasSize(2);
        assertThat(navRow.get(0).getText()).isEqualTo("◀ Назад");
        assertThat(navRow.get(0).getCallbackData()).isEqualTo("history_page_0");
        assertThat(navRow.get(1).getText()).isEqualTo("Далее ▶");
        assertThat(navRow.get(1).getCallbackData()).isEqualTo("history_page_2");
    }

    /**
     * Последняя страница → только кнопка "Назад".
     */
    @Test
    void sendHistoryPage_withLastPage_shouldContainPrevButtonOnly() {
        HistoryEntryDto entry = HistoryEntryDto.builder()
                .taskTitle("3.3. Подзапрос")
                .status("⏳ Не завершено")
                .date(LocalDateTime.now())
                .attempts(0)
                .build();
        int pageNumber = 1;
        int pageSize = 5;
        long totalElements = 8;
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        Page<HistoryEntryDto> page = new PageImpl<>(List.of(entry), pageable, totalElements);
        when(userService.getUserHistory(testUser, pageNumber, pageSize)).thenReturn(page);

        historyService.sendHistoryPage(testUser, chatId, pageNumber);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(updateProducer).producerAnswer(captor.capture());
        SendMessage sent = captor.getValue();

        InlineKeyboardMarkup keyboard = (InlineKeyboardMarkup) sent.getReplyMarkup();
        List<List<InlineKeyboardButton>> rows = keyboard.getKeyboard();
        List<InlineKeyboardButton> navRow = rows.get(0);
        assertThat(navRow).hasSize(1);
        assertThat(navRow.get(0).getText()).isEqualTo("◀ Назад");
        assertThat(navRow.get(0).getCallbackData()).isEqualTo("history_page_0");
    }

    /**
     * Проверка форматирования даты.
     */
    @Test
    void sendHistoryPage_dateFormatting_shouldFormatCorrectly() {
        HistoryEntryDto entry = HistoryEntryDto.builder()
                .taskTitle("4.1. CTE")
                .status("✅ Решено")
                .date(LocalDateTime.of(2024, 12, 25, 8, 5))
                .attempts(3)
                .build();
        Page<HistoryEntryDto> page = new PageImpl<>(List.of(entry));
        when(userService.getUserHistory(testUser, 0, 5)).thenReturn(page);

        historyService.sendHistoryPage(testUser, chatId, 0);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(updateProducer).producerAnswer(captor.capture());
        String text = captor.getValue().getText();
        assertThat(text).contains("25.12.2024 08:05");
    }
}
