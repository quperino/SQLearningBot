package ru.quperino.services.impls;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.BotMessage;
import ru.quperino.entities.UserMessage;
import ru.quperino.repositories.BotMessageRepository;
import ru.quperino.repositories.UserMessageRepository;
import ru.quperino.services.MessageService;

import java.time.LocalDateTime;

/**
 * Реализация {@link MessageService}.
 * Сохраняет входящие и исходящие сообщения в соответствующие таблицы.
 * Данные используются для аналитики и для сброса информации пользователя.
 */
@Service
public class MessageServiceImpl implements MessageService {
    private final UserMessageRepository userMessageRepository;
    private final BotMessageRepository botMessageRepository;

    @Autowired
    public MessageServiceImpl(UserMessageRepository userMessageRepository, BotMessageRepository botMessageRepository) {
        this.userMessageRepository = userMessageRepository;
        this.botMessageRepository = botMessageRepository;
    }

    @Override
    public void saveUserMessage(ApplicationUser user, Message message, Long processingTimeMs) {
        UserMessage userMessage = UserMessage.builder()
                .user(user)
                .messageText(message.getText())
                .createdAt(LocalDateTime.now())
                .processingTimeMs(processingTimeMs)
                .build();
        userMessageRepository.save(userMessage);
    }

    @Override
    public void saveBotMessage(ApplicationUser user, String text, Long processingTimeMs) {
        BotMessage botMessage = BotMessage.builder()
                .user(user)
                .messageText(text)
                .createdAt(LocalDateTime.now())
                .processingTimeMs(processingTimeMs)
                .build();
        botMessageRepository.save(botMessage);
    }
}
