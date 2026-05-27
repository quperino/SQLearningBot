package ru.quperino.services.impls;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.quperino.dto.SolutionCheckRequest;
import ru.quperino.dto.SolutionCheckResponse;
import ru.quperino.services.AIEvaluationService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Реализация {@link AIEvaluationService}.
 * Формирует запрос к OpenAI-совместимому API, отправляет его, парсит ответ.
 * Использует WebClient с тайм-аутом 1 минута.
 */
@Service
@Log4j2
public class AIEvaluationServiceImpl implements AIEvaluationService {
    /** Jackson ObjectMapper для десериализации JSON-ответов от AI. */
    private final ObjectMapper objectMapper;

    /** WebClient для выполнения HTTP-запросов к AI API. */
    private WebClient webClient;

    /** URL AI API (из конфигурации). */
    @Value("${ai.api.url}")
    private String apiUrl;

    /** Ключ API для авторизации (Bearer token). */
    @Value("${ai.api.key}")
    private String apiKey;

    /** Название модели AI (из конфигурации). */
    @Value("${ai.model}")
    private String modelName;

    /** Максимальное количество токенов в ответе AI. */
    @Value("${ai.max.tokens}")
    private int maxTokens;

    /** Температура (креативность) ответов AI. */
    @Value("${ai.temperature}")
    private double temperature;

    @Autowired
    public AIEvaluationServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Выполняется после внедрения зависимостей, до начала работы
    @PostConstruct
    public void init() {
        // Создаём экземпляр WebClient (без базового URL, т.к. URL будет подставляться в каждом запросе)
        this.webClient = WebClient.create();
        log.info("[NODE] AI API URL: {}, model: {}", apiUrl, modelName);
    }

    /**
     * Отправляет запрос на проверку SQL-решения во внешний AI API.
     * <p>
     * Выполняет POST-запрос с тайм-аутом 1 минута. В случае успеха парсит ответ.
     * При тайм-ауте возвращает ответ со статусом "timeout".
     *
     * @param request объект, содержащий текст задачи и SQL пользователя
     * @return объект SolutionCheckResponse с результатом проверки
     */
    @Override
    public SolutionCheckResponse evaluate(SolutionCheckRequest request) {
        // Формируем список сообщений для AI: system-промпт и user-сообщение с задачей и SQL
        List<Map<String, String>> messages = buildMessages(request.getTaskText(), request.getUserSolution());

        // Создаём тело запроса в формате, ожидаемом OpenAI-совместимым API
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);

        // Выполняем POST-запрос к AI API, синхронно ожидая ответ (block) до 1 минуты
        try {
            Map<String, Object> responseBody = webClient.post()
                    // полный URL из конфигурации
                    .uri(apiUrl)
                    // добавляем заголовок авторизации
                    .header("Authorization", "Bearer " + apiKey)
                    // тип содержимого JSON
                    .contentType(MediaType.APPLICATION_JSON)
                    // тело запроса
                    .bodyValue(requestBody)
                    // получаем ответ
                    .retrieve()
                    // превращаем в Mono<Map>
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    // блокируем поток на 1 минуту (тайм-аут)
                    .block(Duration.ofMinutes(1));

            if (responseBody == null) {
                log.error("[NODE] AI API вернул пустой ответ");
                return new SolutionCheckResponse("invalid", "Сервер AI не вернул ответ.", "");
            }

            // Извлекаем текст из ответа OpenAI-совместимого API (поле choices[0].message.content)
            String generatedText = extractTextFromResponse(responseBody);
            if (generatedText == null) {
                log.error("[NODE] Не удалось извлечь текст из ответа AI");
                return new SolutionCheckResponse("invalid", "Не удалось получить ответ от AI.", "");
            }

            log.debug("[NODE] Ответ AI: {}", generatedText);
            // Парсим текст ответа в SolutionCheckResponse (ищем JSON или делаем fallback)
            return parseGeneratedText(generatedText);

        } catch (RuntimeException e) {
            // Если исключение содержит timeout – значит превышен лимит времени
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout")) {
                log.error("[NODE] Таймаут при обращении к AI API (более 1 минуты)", e);
                return new SolutionCheckResponse("timeout", "Проверка решения заняла слишком много времени. Попробуйте ещё раз.", "");
            }
            log.error("[NODE] Ошибка при обращении к AI API", e);
            return new SolutionCheckResponse("invalid", "Не удалось проверить решение из-за технической ошибки.", "");
        } catch (Exception e) {
            log.error("[NODE] Неожиданная ошибка при обращении к AI API", e);
            return new SolutionCheckResponse("invalid", "Не удалось проверить решение из-за технической ошибки.", "");
        }
    }

    /**
     * Извлекает текст ответа AI из структуры OpenAI-совместимого ответа.
     * <p>
     * Ожидаемая структура:
     * <pre>
     * {
     *   "choices": [ { "message": { "content": "...текст..." } } ]
     * }
     * </pre>
     *
     * @param responseBody десериализованный ответ API
     * @return текст из поля content или {@code null}, если извлечь не удалось
     */
    private String extractTextFromResponse(Map<String, Object> responseBody) {
        try {
            // Ответ содержит массив "choices", каждый элемент содержит "message" с "content"
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                return null;
            }
            return (String) message.get("content");
        } catch (ClassCastException e) {
            log.error("[NODE] Ошибка при разборе структуры ответа AI", e);
            return null;
        }
    }

    /**
     * Формирует системный и пользовательский промпты для AI.
     * <p>
     * Системный промпт включает схему базы данных "Library" и подробные правила проверки SQL.
     * Пользовательский промпт содержит условие задачи и SQL-запрос пользователя.
     *
     * @param taskText     текст задачи
     * @param userSolution SQL-запрос пользователя
     * @return список из двух карт: {role="system", ...} и {role="user", ...}
     */
    private List<Map<String, String>> buildMessages(String taskText, String userSolution) {
        String schema = """
                Схема базы данных "Library":
                
                CREATE TABLE authors (
                    id_author SERIAL PRIMARY KEY,
                    surname VARCHAR(100) NOT NULL,
                    name VARCHAR(100) NOT NULL,
                    patronymic VARCHAR(100),
                    birth DATE,
                    death DATE
                );
                
                CREATE TABLE books (
                    id_book SERIAL PRIMARY KEY,
                    name_book VARCHAR(255) NOT NULL,
                    id_author INT REFERENCES authors(id_author) ON DELETE CASCADE,
                    year INT CHECK (year > 0),
                    pages INT DEFAULT 0
                );
                
                CREATE TABLE readers (
                    id_reader SERIAL PRIMARY KEY,
                    surname VARCHAR(100) NOT NULL,
                    name VARCHAR(100) NOT NULL,
                    patronymic VARCHAR(100),
                    birth DATE NOT NULL,
                    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                
                CREATE TABLE loans (
                    id_loan SERIAL PRIMARY KEY,
                    id_book INT REFERENCES books(id_book) ON DELETE CASCADE,
                    id_reader INT REFERENCES readers(id_reader) ON DELETE CASCADE,
                    loan_date DATE NOT NULL,
                    return_date DATE
                );
                
                // Таблица, которая создается в первом задании первого занятия.
                CREATE TABLE publishers (
                    id_publisher SERIAL PRIMARY KEY,
                    name VARCHAR(200) NOT NULL,
                    city VARCHAR(100),
                    founded_year INT CHECK (founded_year >= 1500 AND founded_year <= 2026)
                );
                
                // В первой задаче восьмого занятия создается представление active_books.
                CREATE VIEW active_books AS SELECT * FROM books WHERE pages > 100;
                
                // В первой задаче одиннадцатого занятия создается таблица book_stats
                CREATE TABLE book_stats (
                    id_book INT PRIMARY KEY REFERENCES books(id_book) ON DELETE CASCADE,
                    times_loaned INT DEFAULT 0,
                    last_loan_date DATE
                );
                
                // В третьей задаче одиннадцатого занятия добавляется столбец total_loans в books и обновление столбца total_loans с помощью установки значения из book_stats.times_loaned.
                ALTER TABLE books ADD COLUMN total_loans INT DEFAULT 0;
                UPDATE books SET total_loans = bs.times_loaned FROM book_stats bs WHERE books.id_book = bs.id_book;
                
                // В седьмой задаче одиннадцатого занятия создается представление, которое соединяет loans, readers и books, выбирает фамилию читателя, название книги и дату выдачи для активных выдач (return_date IS NULL).
                CREATE VIEW active_loans AS SELECT r.surname, b.name_book, l.loan_date FROM loans l JOIN readers r ON l.id_reader = r.id_reader JOIN books b ON l.id_book = b.id_book WHERE l.return_date IS NULL;
                """;

        String systemPrompt = """
                Ты — эксперт по SQL. Твоя задача — проверить, является ли пользовательский SQL-запрос корректным и полностью решает ли он поставленную задачу.
                Ты проверяешь запросы, написанные для **PostgreSQL**. Учитывай особенности синтаксиса и поведения PostgreSQL.
                
                %s
                
                ### ДОПОЛНИТЕЛЬНЫЕ ПРАВИЛА POSTGRESQL ###
                Эти правила имеют приоритет над общими ожиданиями. Строго следуйте им при проверке.
                
                1. **Порядок LIMIT и OFFSET**. В PostgreSQL допустимы оба варианта:
                   - `SELECT ... LIMIT n OFFSET m`
                   - `SELECT ... OFFSET m LIMIT n`
                   Они полностью эквивалентны. Не считайте ни один из них ошибкой.
                
                2. **Удаление таблицы с CASCADE**. Команда `DROP TABLE имя_таблицы CASCADE;` абсолютно корректна в PostgreSQL (например, `DROP TABLE publishers CASCADE;`).
                
                3. **Функции CONCAT и CONCAT_WS**. Эти функции корректно обрабатывают NULL и безопасны. Пример: `CONCAT_WS(' ', surname, name, patronymic)`. Одиночные кавычки внутри строки не вызывают ошибок.
                
                4. **Математические функции**. В PostgreSQL существуют функции `FLOOR()`, `CEIL()`, `ROUND()`, `TRUNC()`. Они работают с числами с плавающей точкой. Оба варианта округления вниз до десятков корректны:
                   - `FLOOR(pages / 10) * 10`
                   - `(pages / 10) * 10` (целочисленное деление)
                
                5. **Целочисленное деление**. Оператор `/` между двумя целыми числами даёт целый результат (дробная часть отбрасывается).
                
                6. **Сравнение кортежей с подзапросом**. Синтаксис `WHERE (col1, col2) IN (SELECT col1, col2 FROM ... GROUP BY col1)` является **полностью корректным** в PostgreSQL. Не считайте его ошибочным, если подзапрос содержит `GROUP BY`.
                
                7. **Многокомандные запросы**. В PostgreSQL допускается отправлять несколько SQL-команд в одном запросе, разделяя их точкой с запятой (`;`). Например:
                   - `CREATE VIEW v1 AS ...; CREATE VIEW v2 AS ...; SELECT * FROM v2;`
                   - `CREATE MATERIALIZED VIEW mv AS ...; REFRESH MATERIALIZED VIEW mv;`
                   - `CREATE VIEW ...; SELECT ...;`
                   Такие ответы являются **корректными**, если каждая отдельная команда синтаксически верна и они логически решают задачу. Не отклоняйте решение только из-за наличия нескольких команд.
                
                8. **Пользовательские объекты (таблицы, представления, материализованные представления)**. В процессе обучения пользователь может создавать свои объекты (например, `active_books`, `books_after_1850`, `book_stats`, `mv_author_stats` и т.п.). Отсутствие такого объекта в исходной схеме (`authors`, `books`, `readers`, `loans`, `publishers`) **не является ошибкой**, если пользователь создал его ранее в рамках задания. Единственное исключение – явно выдуманное имя (например, `xyz`, `nonexistent_table`).
                
                9. **Материализованные представления**. Команда `REFRESH MATERIALIZED VIEW имя_представления;` является допустимой и должна признаваться корректной.
                
                10. **Конструкция CASE WHEN**. Синтаксис `CASE WHEN условие THEN значение ... ELSE значение END` полностью поддерживается. Несколько команд в одном запросе (создание представления + выборка) допустимы (см. п.7).
                
                11. **Транзакции и автоматический откат при ошибке**. В PostgreSQL, если внутри блока `BEGIN; ... COMMIT;` возникает любая ошибка (например, нарушение внешнего ключа, синтаксическая ошибка, деление на ноль), вся транзакция немедленно откатывается. Команда `COMMIT` в этом случае не выполняется, а изменения, сделанные до ошибки, не применяются. Пользователю **не требуется** писать явный `ROLLBACK` или использовать `TRY-CATCH`. Простое перечисление команд внутри `BEGIN; ... COMMIT;` с ошибочной вставкой – это **корректное решение** задачи, где требуется показать, что изменения не фиксируются. Например:
                   ```sql
                   BEGIN;
                   UPDATE books SET pages = 100 WHERE id_book = 1;
                   INSERT INTO books (name_book, id_author) VALUES ('Тест', 999);
                   COMMIT;
                
                12. **Изменение типа столбца без USING**. В PostgreSQL допустимо изменять тип столбца без явного USING, если преобразование является неявным (например, из `TIMESTAMP` в `DATE`). Команда `ALTER TABLE readers ALTER COLUMN registered_at TYPE DATE;` синтаксически верна. Не требуйте добавления `USING` для неявных преобразований.
                
                
                ### ПРАВИЛА ОЦЕНКИ И ДЕЙСТВИЯ ПРИ НАРУШЕНИИ ###
                1. **Синтаксическая корректность**: Если запрос содержит синтаксическую ошибку (неправильное написание ключевых слов, пропущенные скобки, лишние запятые и т.п.) → статус "invalid", в message указать конкретную ошибку, в suggestions дать исправленный вариант или совет.
                2. **Существование таблиц/столбцов**: Если в запросе используются таблицы или столбцы, отсутствующие в приведённой выше схеме → статус "invalid", в message перечислить несуществующие объекты, в suggestions предложить заменить на существующие аналоги (если есть) или указать, что таких данных нет.
                3. **Соответствие задаче**: Запрос должен решать именно ту задачу, которая описана в "ЗАДАЧА". Если задача требует сортировки (ORDER BY) — она должна быть. Если фильтрации (WHERE) — должна быть. Если группировки (GROUP BY) — должна быть. Если агрегации (COUNT, SUM, AVG и т.п.) — они должны быть. Если задача не требует конкретных конструкций, они могут отсутствовать. Нарушение → статус "invalid", в message объяснить, чего не хватает, в suggestions дать пример добавления недостающей части.
                4. **Запрет на лишний текст**: Запрос не должен содержать никакого текста, кроме SQL-кода. Если пользователь добавил пояснения на естественном языке или комментарии → статус "invalid", в message указать, что нужен только SQL, в suggestions предложить удалить лишнее.
                5. **Логическая корректность**: Даже если синтаксис верен и таблицы существуют, запрос может быть логически неверен (например, выбирает не те столбцы, неправильно связывает таблицы, использует неподходящее условие). В таком случае → статус "invalid", в message объяснить логическую ошибку, в suggestions дать направление для исправления.
                6. **Если запрос полностью корректен и решает задачу** → статус "valid", в message можно написать краткое подтверждение (например, "Запрос верен"), в suggestions можно оставить пустую строку или дать рекомендации по улучшению стиля/производительности (не обязательно). Давайте рекомендации только в случае, если рекомендация действительно есть. Если с ней решение лучше не станет, то рекомендации не давать.
                
                ### ВАЖНО ###
                - Не выдумывай таблицы и столбцы, которых нет в схеме.
                - Если задача неоднозначна, выбирай наиболее вероятное решение, но в message укажи предположение.
                - Не изменяй запрос пользователя в ответе — только оценивай его.
                
                Твой ответ — ТОЛЬКО JSON, никакого другого текста. Формат:
                {
                  "status": "valid" или "invalid",
                  "message": "краткое пояснение (до 200 символов)",
                  "suggestions": "советы (до 300 символов, без готового запроса) или пустая строка"
                }
                """.formatted(schema);

        // Сообщение пользователя содержит условие задачи и его SQL
        String userPrompt = String.format("ЗАДАЧА: %s\n\nПОЛЬЗОВАТЕЛЬСКИЙ SQL:\n%s", taskText, userSolution);

        // Возвращаем список из двух словарей: системное сообщение и пользовательское
        return List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );
    }

    /**
     * Парсит ответ AI: ищет JSON-фрагмент и преобразует в SolutionCheckResponse.
     * <p>
     * Алгоритм:
     * <ol>
     *   <li>Удаляет Markdown-разметку (```json ... ```).</li>
     *   <li>Ищет первый '{' и последний '}' – извлекает JSON.</li>
     *   <li>Десериализует JSON в Map.</li>
     *   <li>При отсутствии JSON выполняет fallback-парсинг.</li>
     * </ol>
     *
     * @param text "сырой" текст, полученный от AI
     * @return объект SolutionCheckResponse
     */
    private SolutionCheckResponse parseGeneratedText(String text) {
        if (text == null || text.isBlank()) {
            return new SolutionCheckResponse("invalid", "AI не вернул ответ.", "");
        }

        // Очищаем текст: удаляем markdown-разметку (```json ... ``` или просто ```)
        String cleaned = text.trim();
        // убираем начальные ```
        cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\s*", "");
        // убираем конечные ```
        cleaned = cleaned.replaceAll("```$", "");
        // убираем одиночные кавычки-бэктики
        cleaned = cleaned.replaceAll("`", "");

        // Ищем первый '{' и последний '}', чтобы вырезать JSON
        int startJson = cleaned.indexOf('{');
        int endJson = cleaned.lastIndexOf('}');
        String jsonCandidate;
        if (startJson != -1 && endJson != -1 && endJson > startJson) {
            jsonCandidate = cleaned.substring(startJson, endJson + 1);
        } else {
            log.warn("[NODE] Не найден JSON-фрагмент в ответе AI: {}", text);
            // fallback – пытаемся понять по ключевым словам
            return fallbackParse(text);
        }

        try {
            // Пытаемся распарсить JSON в Map
            Map<String, Object> json = objectMapper.readValue(jsonCandidate, new TypeReference<>() {
            });
            String status = (String) json.getOrDefault("status", "invalid");
            String message = (String) json.getOrDefault("message", "");
            String suggestions = (String) json.getOrDefault("suggestions", "");

            if (!"valid".equals(status) && !"invalid".equals(status)) {
                status = "invalid";
            }

            return new SolutionCheckResponse(status, message, suggestions);
        } catch (Exception e) {
            log.warn("[NODE] Не удалось распарсить JSON-фрагмент: {} Ошибка: {}", jsonCandidate, e.getMessage());
            return fallbackParse(text);
        }
    }

    /**
     * Запасной парсинг на случай отсутствия корректного JSON в ответе AI.
     * <p>
     * Анализирует текст на наличие ключевых слов "valid" / "invalid".
     * Если найдено "valid" и нет "invalid" – считает ответ валидным.
     *
     * @param text "сырой" текст от AI
     * @return объект SolutionCheckResponse
     */
    private SolutionCheckResponse fallbackParse(String text) {
        String lower = text.toLowerCase();
        boolean isValid = lower.contains("valid") && !lower.contains("invalid");
        if (isValid) {
            String message = extractMessage(text);
            String suggestions = extractSuggestions(text);
            return new SolutionCheckResponse("valid", message, suggestions);
        } else {
            String shortMsg = text.length() > 200 ? text.substring(0, 200) + "..." : text;
            return new SolutionCheckResponse("invalid", shortMsg, "");
        }
    }

    /**
     * Извлекает первое сообщение из текста (первая строка).
     * Используется в fallback-парсинге.
     *
     * @param text исходный текст
     * @return первая строка или весь текст, если переноса нет
     */
    private String extractMessage(String text) {
        String[] lines = text.split("\n");
        return lines.length > 0 ? lines[0] : text;
    }

    /**
     * Извлекает "советы" из текста (все строки, начиная со второй).
     * Используется в fallback-парсинге.
     *
     * @param text исходный текст
     * @return объединённые через пробел строки, начиная со второй, или пустая строка
     */
    private String extractSuggestions(String text) {
        String[] lines = text.split("\n");
        if (lines.length > 1) {
            return String.join(" ", java.util.Arrays.copyOfRange(lines, 1, lines.length));
        }
        return "";
    }
}
