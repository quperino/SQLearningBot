package ru.quperino.services.enums;

/**
 * Перечисление поддерживаемых команд бота.
 * Используется для сравнения входящего текста с командами.
 */
public enum ServiceCommandsEnum {
    START("/start"),
    REGISTRATION("/registration"),
    CANCEL("/cancel"),
    HELP("/help"),
    RESET("/reset"),
    STATS("/stats"),
    HISTORY("/history"),
    EXPORT("/export"),
    HINT("/hint"),
    MATERIALS("/materials");

    private final String command;

    ServiceCommandsEnum(String command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return command;
    }
}
