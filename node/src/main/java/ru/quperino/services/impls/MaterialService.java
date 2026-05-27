package ru.quperino.services.impls;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Сервис для предоставления PDF-файла методического пособия.
 * Кэширует содержимое после первого чтения.
 */
@Service
public class MaterialService {
    private byte[] cachedPdf;

    /**
     * Возвращает байтовый массив PDF-файла "sql_guide.pdf", который лежит в resources/materials/.
     *
     * @return содержимое PDF
     * @throws IOException если файл не найден или ошибка чтения
     */
    public byte[] getMaterialsPdf() throws IOException {
        if (cachedPdf == null) {
            ClassPathResource resource = new ClassPathResource("materials/sql_guide.pdf");
            cachedPdf = resource.getInputStream().readAllBytes();
        }
        return cachedPdf;
    }
}
