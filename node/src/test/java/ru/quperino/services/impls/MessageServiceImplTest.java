package ru.quperino.services.impls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.BotMessage;
import ru.quperino.entities.UserMessage;
import ru.quperino.repositories.BotMessageRepository;
import ru.quperino.repositories.UserMessageRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Тесты {@link MessageServiceImpl} – проверка сохранения сообщений в базу.
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock private UserMessageRepository userMessageRepository;
    @Mock private BotMessageRepository botMessageRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    private ApplicationUser testUser;
    private Message telegramMessage;

    @BeforeEach
    void setUp() {
        testUser = ApplicationUser.builder().id(1L).build();
        telegramMessage = new Message();
        telegramMessage.setText("Test user message");
    }

    /**
     * Сохранение входящего сообщения: должны быть заполнены user, текст, created_at, время обработки.
     */
    @Test
    void saveUserMessage_shouldSaveUserMessageWithCorrectFields() {
        Long processingTimeMs = 123L;

        messageService.saveUserMessage(testUser, telegramMessage, processingTimeMs);

        ArgumentCaptor<UserMessage> captor = ArgumentCaptor.forClass(UserMessage.class);
        verify(userMessageRepository).save(captor.capture());
        UserMessage saved = captor.getValue();

        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getMessageText()).isEqualTo("Test user message");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getProcessingTimeMs()).isEqualTo(processingTimeMs);
    }

    /**
     * Сохранение исходящего сообщения бота.
     */
    @Test
    void saveBotMessage_shouldSaveBotMessageWithCorrectFields() {
        String botText = "Hello from bot";
        Long processingTimeMs = 456L;

        messageService.saveBotMessage(testUser, botText, processingTimeMs);

        ArgumentCaptor<BotMessage> captor = ArgumentCaptor.forClass(BotMessage.class);
        verify(botMessageRepository).save(captor.capture());
        BotMessage saved = captor.getValue();

        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getMessageText()).isEqualTo(botText);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getProcessingTimeMs()).isEqualTo(processingTimeMs);
    }
}
