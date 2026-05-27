package ru.quperino.services;

import ru.quperino.entities.ApplicationUser;

/**
 * Сервис для генерации CSV-файла с историей решений пользователя.
 */
public interface ExportService {
    /**
     * Создаёт CSV-файл (в виде байтового массива) со всеми решениями пользователя.
     * Добавляет BOM (U+FEFF) для корректного отображения кириллицы в Excel.
     *
     * @param user пользователь
     * @return байтовый массив CSV-файла
     */
    byte[] generateExportCsv(ApplicationUser user);
}
