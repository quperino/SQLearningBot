-- *************************************
-- СЕКЦИЯ МЕТОДИЧКА (ЗАНЯТИЯ 1–15) *****
-- *************************************

-- =====================================
-- Занятие 1. Создание и удаление таблиц
-- =====================================

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 1, 'Занятие 1. Создание и удаление таблиц в PostgreSQL', 1,
       '1.1. Создание таблицы издательств',
       'Напишите команду CREATE TABLE для таблицы publishers (издательства) со следующими полями:
    - id_publisher – первичный ключ, автоинкремент;
    - name – название издательства, строка до 200 символов, не пустое;
    - city – город, строка до 100 символов;
    - founded_year – год основания, целое число (проверка, что год не меньше 1500 и не больше текущего года (текущий год укажите статически)).',
       5,
       'Используйте SERIAL для автоинкремента, PRIMARY KEY и NOT NULL для названия. Добавьте CHECK, чтобы год основания был не меньше 1500 и не больше текущего года (укажите текущий год статически, например, 2026).',
       'CREATE TABLE publishers (id_publisher SERIAL PRIMARY KEY, name VARCHAR(200) NOT NULL, city VARCHAR(100), founded_year INT CHECK (founded_year >= 1500 AND founded_year <= 2026));'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 1 AND task_number = 1);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 1, 'Занятие 1. Создание и удаление таблиц в PostgreSQL', 2,
       '1.2. Добавление столбца',
       'Добавьте в таблицу books столбец isbn типа VARCHAR(13) (международный стандартный книжный номер).',
       5,
       'Используйте команду ALTER TABLE с опцией ADD COLUMN. Укажите имя столбца и его тип данных.',
       'ALTER TABLE books ADD COLUMN isbn VARCHAR(13);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 1 AND task_number = 2);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 1, 'Занятие 1. Создание и удаление таблиц в PostgreSQL', 3,
       '1.3. Удаление столбца',
       'Удалите из таблицы books столбец pages, который был добавлен в примере (предполагается, что он существует).',
       5,
       'Используйте ALTER TABLE с опцией DROP COLUMN. После имени столбца ничего дополнительно указывать не нужно.',
       'ALTER TABLE books DROP COLUMN pages;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 1 AND task_number = 3);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 1, 'Занятие 1. Создание и удаление таблиц в PostgreSQL', 4,
       '1.4. Изменение типа столбца',
       'В таблице readers измените тип столбца registered_at с TIMESTAMP на DATE (дата без времени).',
       5,
       'Примените ALTER TABLE ... ALTER COLUMN ... TYPE. Укажите новый тип DATE. Убедитесь, что существующие значения можно безопасно преобразовать (PostgreSQL автоматически отбросит временную часть).',
       'ALTER TABLE readers ALTER COLUMN registered_at TYPE DATE;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 1 AND task_number = 4);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 1, 'Занятие 1. Создание и удаление таблиц в PostgreSQL', 5,
       '1.5. Создание таблицы со связью',
       'Создайте таблицу loans (выдача книг читателям), которая содержит:
    - id_loan – первичный ключ;
    - id_book – внешний ключ на books(id_book);
    - id_reader – внешний ключ на readers(id_reader);
    - loan_date – дата выдачи, тип DATE (по умолчанию текущая дата);
    - return_date – дата возврата, может быть пустой.
Используйте ON DELETE CASCADE для внешних ключей.',
       5,
       'Создайте таблицу с первичным ключом-счётчиком. Добавьте два внешних ключа с каскадным удалением. Поле loan_date сделайте с текущей датой по умолчанию, поле return_date может быть пустым.',
       'CREATE TABLE loans (id_loan SERIAL PRIMARY KEY, id_book INT REFERENCES books(id_book) ON DELETE CASCADE, id_reader INT REFERENCES readers(id_reader) ON DELETE CASCADE, loan_date DATE DEFAULT CURRENT_DATE, return_date DATE);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 1 AND task_number = 5);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 1, 'Занятие 1. Создание и удаление таблиц в PostgreSQL', 6,
       '1.6. Удаление таблицы с зависимостями',
       'Напишите команду, которая удаляет таблицу publishers вместе со всеми объектами, которые на неё ссылаются (если бы такие были). Используйте CASCADE.',
       5,
       'Примените DROP TABLE с опцией CASCADE. Это удалит не только саму таблицу, но и все зависящие от неё объекты (например, внешние ключи, представления).',
       'DROP TABLE publishers CASCADE;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 1 AND task_number = 6);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 1, 'Занятие 1. Создание и удаление таблиц в PostgreSQL', 7,
       '1.7. Создание временной таблицы',
       'Создайте временную таблицу temp_books с одним столбцом temp_name типа TEXT. Временная таблица должна автоматически удалиться после завершения сессии.',
       5,
       'Используйте CREATE TEMP TABLE (или CREATE TEMPORARY TABLE). Укажите имя таблицы и определение столбца. Таблица будет существовать только в рамках текущей сессии и удалится при её закрытии.',
       'CREATE TEMP TABLE temp_books (temp_name TEXT);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 1 AND task_number = 7);


-- =========================================================================
-- Занятие 2. Вставка, обновление и удаление данных (INSERT, UPDATE, DELETE)
-- =========================================================================

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 2, 'Занятие 2. Вставка, обновление и удаление данных (INSERT, UPDATE, DELETE)', 1,
       '2.1. Вставка одного автора',
       'Добавьте в таблицу authors автора с фамилией «Гоголь», именем «Николай», отчеством «Васильевич». Год рождения – 1809, год смерти – 1852. Используйте явный перечень столбцов.',
       5,
       'Укажите явно список столбцов (кроме id_author, который заполнится сам). Значения дат записывайте в формате ГГГГ-ММ-ДД.',
       'INSERT INTO authors (surname, name, patronymic, birth, death) VALUES (''Гоголь'', ''Николай'', ''Васильевич'', ''1809-03-20'', ''1852-02-21'');'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 2 AND task_number = 1);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 2, 'Занятие 2. Вставка, обновление и удаление данных (INSERT, UPDATE, DELETE)', 2,
       '2.2. Вставка нескольких книг',
       'Добавьте в таблицу books три книги автора с id_author = 1 (Пушкин):
    - «Сказка о царе Салтане» (год 1831, страниц 120),
    - «Медный всадник» (год 1833, страниц 80),
    - «Борис Годунов» (год 1831, страниц 150).
Напишите один запрос INSERT, который добавляет все три книги.',
       5,
       'Используйте один INSERT с несколькими наборами значений через запятую. Не забудьте указать, какие столбцы вы заполняете.',
       'INSERT INTO books (name_book, id_author, year, pages) VALUES (''Сказка о царе Салтане'', 1, 1831, 120), (''Медный всадник'', 1, 1833, 80), (''Борис Годунов'', 1, 1831, 150);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 2 AND task_number = 2);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 2, 'Занятие 2. Вставка, обновление и удаление данных (INSERT, UPDATE, DELETE)', 3,
       '2.3. Обновление данных (с условием)',
       'У книги «Война и мир» измените количество страниц на 1300. Используйте WHERE по названию книги.',
       5,
       'Напишите UPDATE, указав новое значение для столбца pages. В условии WHERE отфильтруйте книгу по её названию.',
       'UPDATE books SET pages = 1300 WHERE name_book = ''Война и мир'';'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 2 AND task_number = 3);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 2, 'Занятие 2. Вставка, обновление и удаление данных (INSERT, UPDATE, DELETE)', 4,
       '2.4. Обновление всех строк (осторожно!)',
       'Напишите запрос, который увеличивает год издания всех книг на 1.',
       5,
       'UPDATE без WHERE действует на все строки. Чтобы увеличить значение столбца, используйте SET, где справа от знака равенства обратитесь к текущему значению этого столбца и прибавьте 1.',
       'UPDATE books SET year = year + 1;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 2 AND task_number = 4);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 2, 'Занятие 2. Вставка, обновление и удаление данных (INSERT, UPDATE, DELETE)', 5,
       '2.5. Удаление по условию',
       'Удалите из таблицы books все книги, у которых количество страниц меньше 100.',
       5,
       'Напишите DELETE с условием, которое выбирает книги с количеством страниц меньше 100.',
       'DELETE FROM books WHERE pages < 100;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 2 AND task_number = 5);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 2, 'Занятие 2. Вставка, обновление и удаление данных (INSERT, UPDATE, DELETE)', 6,
       '2.6. Удаление с подзапросом',
       'Удалите из таблицы authors всех авторов, у которых нет книг.',
       5,
       'Используйте DELETE FROM authors WHERE id_author NOT IN (подзапрос). Подзапрос должен возвращать все id_author из таблицы books. Важно исключить NULL в подзапросе, иначе NOT IN не сработает – добавьте WHERE id_author IS NOT NULL.',
       'DELETE FROM authors WHERE id_author NOT IN (SELECT DISTINCT id_author FROM books WHERE id_author IS NOT NULL);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 2 AND task_number = 6);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 2, 'Занятие 2. Вставка, обновление и удаление данных (INSERT, UPDATE, DELETE)', 7,
       '2.7. Использование RETURNING',
       'Добавьте нового читателя с фамилией «Сидоров», именем «Алексей», отчеством «Петрович», датой рождения «2000-01-01» и верните его сгенерированный id_reader одним запросом.',
       5,
       'После INSERT добавьте RETURNING id_reader. Это вернёт значение, которое автоматически создалось для первичного ключа (SERIAL).',
       'INSERT INTO readers (surname, name, patronymic, birth) VALUES (''Сидоров'', ''Алексей'', ''Петрович'', ''2000-01-01'') RETURNING id_reader;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 2 AND task_number = 7);


-- =========================================
-- Занятие 3. Выборка данных: команда SELECT
-- =========================================

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 3, 'Занятие 3. Выборка данных: команда SELECT', 1,
       '3.1. Выборка всех столбцов',
       'Напишите запрос, который выводит все данные из таблицы readers.',
       5,
       'Используйте SELECT * FROM имя_таблицы.',
       'SELECT * FROM readers;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 3 AND task_number = 1);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 3, 'Занятие 3. Выборка данных: команда SELECT', 2,
       '3.2. Выборка конкретных столбцов',
       'Выведите фамилию, имя и дату рождения всех читателей.',
       5,
       'Перечислите нужные столбцы после SELECT через запятую.',
       'SELECT surname, name, birth FROM readers;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 3 AND task_number = 2);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 3, 'Занятие 3. Выборка данных: команда SELECT', 3,
       '3.3. Устранение дубликатов',
       'Выведите уникальные годы рождения читателей. Отсортируйте результат по возрастанию года.',
       5,
       'Используйте DISTINCT, чтобы убрать дубликаты. Отсортируйте результат по возрастанию года рождения.',
       'SELECT DISTINCT birth FROM readers ORDER BY birth;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 3 AND task_number = 3);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 3, 'Занятие 3. Выборка данных: команда SELECT', 4,
       '3.4. Фильтрация по условию (один оператор)',
       'Выведите названия книг, у которых количество страниц больше 500.',
       5,
       'Отфильтруйте книги по количеству страниц: оставьте только те, у которых страниц больше 500.',
       'SELECT name_book FROM books WHERE pages > 500;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 3 AND task_number = 4);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 3, 'Занятие 3. Выборка данных: команда SELECT', 5,
       '3.5. Фильтрация по условию (два оператора)',
       'Выведите названия и годы издания книг, написанных автором с id_author = 2 (Толстой), но изданных не ранее 1870 года.',
       5,
       'Используйте два условия, соединённые через AND: нужный автор и год не ранее 1870.',
       'SELECT name_book, year FROM books WHERE id_author = 2 AND year >= 1870;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 3 AND task_number = 5);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 3, 'Занятие 3. Выборка данных: команда SELECT', 6,
       '3.6. Проверка на NULL',
       'Выведите всех авторов, у которых заполнена дата смерти (death IS NOT NULL).',
       5,
       'В SQL NULL проверяется через IS NULL / IS NOT NULL, а не через =.',
       'SELECT * FROM authors WHERE death IS NOT NULL;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 3 AND task_number = 6);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 3, 'Занятие 3. Выборка данных: команда SELECT', 7,
       '3.7. Использование IN',
       'Выведите названия книг, у которых id_author равен 1 или 3.',
       5,
       'Используйте IN с перечислением нужных id_author (1 и 3).',
       'SELECT name_book FROM books WHERE id_author IN (1, 3);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 3 AND task_number = 7);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 3, 'Занятие 3. Выборка данных: команда SELECT', 8,
       '3.8. Использование BETWEEN',
       'Выведите книги, изданные в период с 1830 по 1850 год включительно.',
       5,
       'Используйте BETWEEN для диапазона годов с 1830 по 1850 включительно.',
       'SELECT * FROM books WHERE year BETWEEN 1830 AND 1850;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 3 AND task_number = 8);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 3, 'Занятие 3. Выборка данных: команда SELECT', 9,
       '3.9. Поиск по шаблону (LIKE)',
       'Выведите фамилии авторов, которые начинаются на букву «Д» (без учёта регистра, используйте ILIKE).',
       5,
       'Используйте ILIKE с шаблоном, начинающимся на букву «Д». Символ % обозначает любые последующие символы.',
       'SELECT surname FROM authors WHERE surname ILIKE ''Д%'';'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 3 AND task_number = 9);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 3, 'Занятие 3. Выборка данных: команда SELECT', 10,
       '3.10. Вычисления и псевдонимы',
       'Выведите название книги и количество страниц, уменьшенное на 10% (как pages_reduced). Результат округлите до целого числа.',
       5,
       'Уменьшите значение pages на 10% с помощью умножения, округлите результат до целого и дайте столбцу псевдоним pages_reduced.',
       'SELECT name_book, ROUND(pages * 0.9) AS pages_reduced FROM books;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 3 AND task_number = 10);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 3, 'Занятие 3. Выборка данных: команда SELECT', 11,
       '3.11. Конкатенация строк',
       'Выведите полное имя читателя (фамилию, имя и отчество) в одном столбце с псевдонимом full_name. Если отчество отсутствует (NULL), выведите только фамилию и имя.',
       5,
       'Используйте функцию CONCAT или CONCAT_WS для безопасной конкатенации с учётом NULL.',
       'SELECT CONCAT_WS('' '', surname, name, patronymic) AS full_name FROM readers;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 3 AND task_number = 11);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 3, 'Занятие 3. Выборка данных: команда SELECT', 12,
       '3.12. Комбинированный запрос',
       'Выведите уникальные фамилии авторов, у которых год рождения позже 1800 года, отсортированные по фамилии в алфавитном порядке.',
       5,
       'Отберите уникальные фамилии авторов, родившихся после 1800 года, и отсортируйте их по алфавиту.',
       'SELECT DISTINCT surname FROM authors WHERE birth > ''1800-01-01'' ORDER BY surname;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 3 AND task_number = 12);


-- ========================================================================
-- Занятие 4. Сортировка и ограничение количества записей (ORDER BY, LIMIT)
-- ========================================================================

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 4, 'Занятие 4. Сортировка и ограничение количества записей (ORDER BY, LIMIT)', 1,
       '4.1. Простая сортировка по возрастанию',
       'Выведите названия книг и годы издания, отсортированные по году (от старых к новым).',
       5,
       'Отсортируйте результат по году от старых к новым. Направление сортировки по умолчанию – возрастание.',
       'SELECT name_book, year FROM books ORDER BY year;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 4 AND task_number = 1);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 4, 'Занятие 4. Сортировка и ограничение количества записей (ORDER BY, LIMIT)', 2,
       '4.2. Сортировка по убыванию',
       'Выведите фамилии, имена и даты рождения читателей, отсортированные по дате рождения (самые молодые – первые).',
       5,
       'Отсортируйте читателей по дате рождения так, чтобы самые молодые были первыми (убывающий порядок).',
       'SELECT surname, name, birth FROM readers ORDER BY birth DESC;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 4 AND task_number = 2);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 4, 'Занятие 4. Сортировка и ограничение количества записей (ORDER BY, LIMIT)', 3,
       '4.3. Сортировка по двум полям',
       'Выведите названия книг, id автора и год издания, отсортированные сначала по id автора (по возрастанию), а внутри одного автора – по году (по убыванию).',
       5,
       'Сначала отсортируйте по id автора (по возрастанию), а внутри каждого автора – по году издания от новых к старым.',
       'SELECT name_book, id_author, year FROM books ORDER BY id_author, year DESC;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 4 AND task_number = 3);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 4, 'Занятие 4. Сортировка и ограничение количества записей (ORDER BY, LIMIT)', 4,
       '4.4. Сортировка с NULL',
       'Выведите фамилию, имя и отчество авторов, отсортированные по отчеству так, чтобы NULL шли в начале списка.',
       5,
       'Добавьте NULLS FIRST в ORDER BY. В PostgreSQL NULL по умолчанию считаются больше любого значения, но это поведение можно изменить.',
       'SELECT surname, name, patronymic FROM authors ORDER BY patronymic NULLS FIRST;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 4 AND task_number = 4);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 4, 'Занятие 4. Сортировка и ограничение количества записей (ORDER BY, LIMIT)', 5,
       '4.5. Простой LIMIT',
       'Выведите первые 5 записей из таблицы books (без сортировки, но для определённости используйте ORDER BY id_book).',
       5,
       'Для определённого порядка используйте ORDER BY (например, по первичному ключу). Ограничьте вывод 5 записями с помощью LIMIT.',
       'SELECT * FROM books ORDER BY id_book LIMIT 5;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 4 AND task_number = 5);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 4, 'Занятие 4. Сортировка и ограничение количества записей (ORDER BY, LIMIT)', 6,
       '4.6. LIMIT с сортировкой',
       'Выведите 3 книги с наибольшим количеством страниц. Результат должен содержать название и количество страниц.',
       5,
       'Отсортируйте книги по убыванию количества страниц и выведите только первые три.',
       'SELECT name_book, pages FROM books ORDER BY pages DESC LIMIT 3;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 4 AND task_number = 6);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 4, 'Занятие 4. Сортировка и ограничение количества записей (ORDER BY, LIMIT)', 7,
       '4.7. Пропуск записей (OFFSET)',
       'Пропустите первые 2 книги и выведите следующие 4 книги, отсортированные по названию.',
       5,
       'Отсортируйте книги по названию. Пропустите первые две строки (OFFSET) и выведите следующие четыре (LIMIT).',
       'SELECT * FROM books ORDER BY name_book OFFSET 2 LIMIT 4;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 4 AND task_number = 7);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 4, 'Занятие 4. Сортировка и ограничение количества записей (ORDER BY, LIMIT)', 8,
       '4.8. Постраничный вывод',
       'Напишите запрос, который выводит вторую страницу списка авторов (по 3 автора на странице), отсортированных по фамилии. Результат должен содержать фамилию и имя.',
       5,
       'Отсортируйте авторов по фамилии. Для второй страницы (по 3 записи) пропустите первые 3 записи и возьмите следующие 3.',
       'SELECT surname, name FROM authors ORDER BY surname LIMIT 3 OFFSET 3;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 4 AND task_number = 8);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 4, 'Занятие 4. Сортировка и ограничение количества записей (ORDER BY, LIMIT)', 9,
       '4.9. Комбинированный запрос (фильтрация + сортировка + LIMIT)',
       'Выведите названия книг, изданных после 1850 года, отсортированные по году (от новых к старым), и возьмите только первые 2 записи.',
       5,
       'Сначала оставьте только книги, изданные после 1850 года. Затем отсортируйте их по убыванию года и возьмите первые две.',
       'SELECT name_book FROM books WHERE year > 1850 ORDER BY year DESC LIMIT 2;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 4 AND task_number = 9);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 4, 'Занятие 4. Сортировка и ограничение количества записей (ORDER BY, LIMIT)', 10,
       '4.10. Использование FETCH FIRST',
       'Перепишите задачу 6 с использованием стандартного синтаксиса FETCH FIRST 3 ROWS ONLY.',
       5,
       'Используйте стандартный синтаксис FETCH FIRST вместо LIMIT. Укажите количество строк после FETCH FIRST и ключевое слово ROWS ONLY.',
       'SELECT name_book, pages FROM books ORDER BY pages DESC FETCH FIRST 3 ROWS ONLY;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 4 AND task_number = 10);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 4, 'Занятие 4. Сортировка и ограничение количества записей (ORDER BY, LIMIT)', 11,
       '4.11. Сложная сортировка с вычислениями',
       'Выведите название книги и количество страниц, округлённое до десятков (в меньшую сторону). Отсортируйте результат по этому округлённому значению по убыванию, а затем по названию.',
       5,
       'Округлите количество страниц вниз до десятков (например, 123 → 120). Отсортируйте сначала по этому округлённому значению по убыванию, затем по названию книги.',
       'SELECT name_book, FLOOR(pages / 10) * 10 AS pages_rounded FROM books ORDER BY pages_rounded DESC, name_book;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 4 AND task_number = 11);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 4, 'Занятие 4. Сортировка и ограничение количества записей (ORDER BY, LIMIT)', 12,
       '4.12. Проверка на прочность',
       'Выведите уникальные фамилии авторов, у которых год рождения позже 1800 года, отсортированные по длине фамилии (от самых коротких к самым длинным). Если длины совпадают – по алфавиту.',
       5,
       'Отберите уникальные фамилии авторов, родившихся после 1800 года. Отсортируйте их сначала по длине фамилии (от самой короткой), а при одинаковой длине – по алфавиту.',
       'SELECT DISTINCT surname FROM authors WHERE birth > ''1800-01-01'' ORDER BY LENGTH(surname), surname;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 4 AND task_number = 12);


-- ===============================================================================
-- Занятие 5. Агрегатные функции и группировка (COUNT, SUM, AVG, GROUP BY, HAVING)
-- ===============================================================================

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 5, 'Занятие 5. Агрегатные функции и группировка (COUNT, SUM, AVG, GROUP BY, HAVING)', 1,
       '5.1. Общее количество читателей',
       'Подсчитайте, сколько читателей зарегистрировано в таблице readers.',
       5,
       'Примените агрегатную функцию, которая подсчитывает количество всех строк в таблице, включая NULL.',
       'SELECT COUNT(*) FROM readers;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 5 AND task_number = 1);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 5, 'Занятие 5. Агрегатные функции и группировка (COUNT, SUM, AVG, GROUP BY, HAVING)', 2,
       '5.2. Количество книг без страниц',
       'Подсчитайте, сколько книг не имеют указанного количества страниц (pages IS NULL).',
       5,
       'Подсчитайте количество книг, у которых поле pages не заполнено. Используйте условие в WHERE и функцию COUNT(*).',
       'SELECT COUNT(*) FROM books WHERE pages IS NULL;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 5 AND task_number = 2);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 5, 'Занятие 5. Агрегатные функции и группировка (COUNT, SUM, AVG, GROUP BY, HAVING)', 3,
       '5.3. Суммарное количество страниц',
       'Найдите общее количество страниц во всех книгах.',
       5,
       'Используйте функцию, которая суммирует значения в столбце pages (NULL игнорируются).',
       'SELECT SUM(pages) FROM books;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 5 AND task_number = 3);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 5, 'Занятие 5. Агрегатные функции и группировка (COUNT, SUM, AVG, GROUP BY, HAVING)', 4,
       '5.4. Средний год издания книг',
       'Вычислите средний год издания книг в таблице books. Результат округлите до целого числа.',
       5,
       'Вычислите средний год издания с помощью агрегатной функции, затем округлите результат до целого.',
       'SELECT ROUND(AVG(year)) FROM books;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 5 AND task_number = 4);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 5, 'Занятие 5. Агрегатные функции и группировка (COUNT, SUM, AVG, GROUP BY, HAVING)', 5,
       '5.5. Самая толстая и самая тонкая книга',
       'Найдите максимальное и минимальное количество страниц среди всех книг.',
       5,
       'Найдите максимальное и минимальное значение pages в одном запросе, используя соответствующие агрегатные функции.',
       'SELECT MAX(pages), MIN(pages) FROM books;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 5 AND task_number = 5);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 5, 'Занятие 5. Агрегатные функции и группировка (COUNT, SUM, AVG, GROUP BY, HAVING)', 6,
       '5.6. Количество книг по авторам',
       'Выведите id_author и количество книг каждого автора. Отсортируйте по убыванию количества.',
       5,
       'Сгруппируйте книги по автору, посчитайте количество в каждой группе. Отсортируйте результат по убыванию этого количества.',
       'SELECT id_author, COUNT(*) FROM books GROUP BY id_author ORDER BY COUNT(*) DESC;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 5 AND task_number = 6);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 5, 'Занятие 5. Агрегатные функции и группировка (COUNT, SUM, AVG, GROUP BY, HAVING)', 7,
       '5.7. Средний год издания по авторам',
       'Выведите id_author и средний год издания его книг. Округлите среднее до целого.',
       5,
       'Сгруппируйте книги по автору, вычислите средний год издания в каждой группе и округлите его.',
       'SELECT id_author, ROUND(AVG(year)) FROM books GROUP BY id_author;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 5 AND task_number = 7);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 5, 'Занятие 5. Агрегатные функции и группировка (COUNT, SUM, AVG, GROUP BY, HAVING)', 8,
       '5.8. Фильтрация групп (HAVING)',
       'Выведите id_author, у которых общее количество страниц (SUM(pages)) больше 1000. Отсортируйте по убыванию суммы.',
       5,
       'После группировки отфильтруйте группы с помощью HAVING, оставив только те, у которых сумма страниц больше 1000.',
       'SELECT id_author, SUM(pages) FROM books GROUP BY id_author HAVING SUM(pages) > 1000 ORDER BY SUM(pages) DESC;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 5 AND task_number = 8);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 5, 'Занятие 5. Агрегатные функции и группировка (COUNT, SUM, AVG, GROUP BY, HAVING)', 9,
       '5.9. Комбинация WHERE и HAVING',
       'Выведите id_author и количество книг, изданных после 1830 года, но только для тех авторов, у которых таких книг больше 1.',
       5,
       'Сначала оставьте только книги, изданные после 1830 года. Затем сгруппируйте по автору и оставьте только те группы, где количество таких книг больше 1.',
       'SELECT id_author, COUNT(*) FROM books WHERE year > 1830 GROUP BY id_author HAVING COUNT(*) > 1;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 5 AND task_number = 9);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 5, 'Занятие 5. Агрегатные функции и группировка (COUNT, SUM, AVG, GROUP BY, HAVING)', 10,
       '5.10. Использование COUNT(DISTINCT)',
       'Подсчитайте, сколько уникальных годов издания представлено в таблице books.',
       5,
       'Используйте COUNT с DISTINCT, чтобы подсчитать количество уникальных годов издания.',
       'SELECT COUNT(DISTINCT year) FROM books;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 5 AND task_number = 10);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 5, 'Занятие 5. Агрегатные функции и группировка (COUNT, SUM, AVG, GROUP BY, HAVING)', 11,
       '5.11. Сложная агрегация',
       'Выведите максимальное, минимальное и среднее количество страниц для книг, изданных в XIX веке (1801–1900). Назовите столбцы max_pages, min_pages, avg_pages.',
       5,
       'Отфильтруйте книги XIX века (1801–1900), затем примените агрегатные функции MAX, MIN, AVG к pages, дав столбцам понятные имена.',
       'SELECT MAX(pages) AS max_pages, MIN(pages) AS min_pages, AVG(pages) AS avg_pages FROM books WHERE year BETWEEN 1801 AND 1900;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 5 AND task_number = 11);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 5, 'Занятие 5. Агрегатные функции и группировка (COUNT, SUM, AVG, GROUP BY, HAVING)', 12,
       '5.12. Группировка с вычислением',
       'Для каждого года издания выведите количество книг, общее количество страниц и среднее количество страниц. Отсортируйте по году.',
       5,
       'Сгруппируйте книги по году издания. Для каждой группы выведите количество книг, общую сумму страниц и среднее количество страниц. Отсортируйте по году.',
       'SELECT year, COUNT(*) AS book_count, SUM(pages) AS total_pages, AVG(pages) AS avg_pages FROM books GROUP BY year ORDER BY year;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 5 AND task_number = 12);


-- ====================================
-- Занятие 6. Объединение таблиц (JOIN)
-- ====================================

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 6, 'Занятие 6. Объединение таблиц (JOIN)', 1,
       '6.1. INNER JOIN (книги + авторы)',
       'Выведите название книги, фамилию и имя автора для всех книг, у которых указан автор. Используйте INNER JOIN.',
       10,
       'Соедините таблицы books и authors по полю id_author. INNER JOIN оставляет только строки с совпадением в обеих таблицах.',
       'SELECT b.name_book, a.surname, a.name FROM books b INNER JOIN authors a ON b.id_author = a.id_author;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 6 AND task_number = 1);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 6, 'Занятие 6. Объединение таблиц (JOIN)', 2,
       '6.2. LEFT JOIN (авторы + книги)',
       'Выведите фамилию автора и название его книги. Включите всех авторов, даже если у них нет книг (тогда название книги будет NULL).',
       10,
       'Используйте LEFT JOIN: authors — левая таблица, books — правая. Все строки из authors сохранятся.',
       'SELECT a.surname, b.name_book FROM authors a LEFT JOIN books b ON a.id_author = b.id_author;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 6 AND task_number = 2);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 6, 'Занятие 6. Объединение таблиц (JOIN)', 3,
       '6.3. JOIN с сортировкой',
       'Выведите название книги, фамилию автора и год издания, отсортированные по фамилии автора, а затем по году (от старых к новым).',
       10,
       'Отсортируйте результат по фамилии автора, затем по году издания (от старых к новым).',
       'SELECT b.name_book, a.surname, b.year FROM books b JOIN authors a ON b.id_author = a.id_author ORDER BY a.surname, b.year;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 6 AND task_number = 3);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 6, 'Занятие 6. Объединение таблиц (JOIN)', 4,
       '6.4. JOIN с агрегацией (количество книг по авторам)',
       'Выведите фамилию автора и количество написанных им книг. Используйте JOIN и GROUP BY. Авторов без книг не выводить.',
       10,
       'INNER JOIN books с authors, GROUP BY a.id_author, a.surname, затем COUNT(b.id_book).',
       'SELECT a.surname, COUNT(b.id_book) FROM authors a JOIN books b ON a.id_author = b.id_author GROUP BY a.id_author, a.surname;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 6 AND task_number = 4);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 6, 'Занятие 6. Объединение таблиц (JOIN)', 5,
       '6.5. LEFT JOIN с агрегацией (включая авторов без книг)',
       'Выведите фамилию автора и количество книг. Если у автора нет книг, количество должно быть 0.',
       10,
       'LEFT JOIN, GROUP BY, и COUNT(b.id_book) — при отсутствии книг счётчик даст 0.',
       'SELECT a.surname, COUNT(b.id_book) FROM authors a LEFT JOIN books b ON a.id_author = b.id_author GROUP BY a.id_author, a.surname;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 6 AND task_number = 5);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 6, 'Занятие 6. Объединение таблиц (JOIN)', 6,
       '6.6. JOIN с фильтрацией',
       'Выведите названия книг и фамилии авторов, у которых год издания книги позже 1850 года.',
       10,
       'Добавьте условие, чтобы год издания книги был позже 1850.',
       'SELECT b.name_book, a.surname FROM books b JOIN authors a ON b.id_author = a.id_author WHERE b.year > 1850;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 6 AND task_number = 6);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 6, 'Занятие 6. Объединение таблиц (JOIN)', 7,
       '6.7. Объединение трёх таблиц (через loans)',
       'Выведите фамилию читателя, название книги и дату выдачи для всех записей в таблице loans. Используйте два JOIN.',
       10,
       'Соедините loans с readers (по id_reader) и loans с books (по id_book). Выберите нужные столбцы.',
       'SELECT r.surname, b.name_book, l.loan_date FROM loans l JOIN readers r ON l.id_reader = r.id_reader JOIN books b ON l.id_book = b.id_book;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 6 AND task_number = 7);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 6, 'Занятие 6. Объединение таблиц (JOIN)', 8,
       '6.8. JOIN с вычисляемым столбцом',
       'Выведите название книги, фамилию автора и количество страниц, увеличенное на 10% (как pages_plus_percent). Округлите до целого.',
       10,
       'Увеличьте количество страниц на 10% и округлите до целого. Дайте столбцу псевдоним.',
       'SELECT b.name_book, a.surname, ROUND(b.pages * 1.1) AS pages_plus_percent FROM books b JOIN authors a ON b.id_author = a.id_author;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 6 AND task_number = 8);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 6, 'Занятие 6. Объединение таблиц (JOIN)', 9,
       '6.9. Группировка с JOIN и HAVING',
       'Выведите фамилию автора и средний год издания его книг, но только для тех авторов, у которых средний год позже 1850 года.',
       10,
       'Сгруппируйте по автору, вычислите средний год издания его книг и оставьте только тех, у кого средний год позже 1850.',
       'SELECT a.surname, AVG(b.year) FROM authors a JOIN books b ON a.id_author = b.id_author GROUP BY a.id_author, a.surname HAVING AVG(b.year) > 1850;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 6 AND task_number = 9);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 6, 'Занятие 6. Объединение таблиц (JOIN)', 10,
       '6.10. Самый плодовитый автор',
       'Выведите фамилию автора и количество его книг, отсортируйте по убыванию количества и оставьте только одного (самого плодовитого). Используйте JOIN, GROUP BY, ORDER BY и LIMIT.',
       10,
       'GROUP BY a.id_author, a.surname, ORDER BY COUNT(b.id_book) DESC, LIMIT 1.',
       'SELECT a.surname, COUNT(b.id_book) FROM authors a JOIN books b ON a.id_author = b.id_author GROUP BY a.id_author, a.surname ORDER BY COUNT(b.id_book) DESC LIMIT 1;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 6 AND task_number = 10);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 6, 'Занятие 6. Объединение таблиц (JOIN)', 11,
       '6.11. Книги без авторов (проверка ссылочной целостности)',
       'Выведите все книги, у которых id_author не соответствует ни одному id_author в таблице authors (например, если книга ссылается на несуществующего автора). Используйте LEFT JOIN и проверку WHERE authors.id_author IS NULL.',
       10,
       'Используйте LEFT JOIN и проверьте, что в таблице authors нет соответствующей записи.',
       'SELECT b.* FROM books b LEFT JOIN authors a ON b.id_author = a.id_author WHERE a.id_author IS NULL;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 6 AND task_number = 11);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 6, 'Занятие 6. Объединение таблиц (JOIN)', 12,
       '6.12. Полная информация о книге (автор + если есть читатели)',
       'Выведите название книги, фамилию автора, фамилию читателя и дату выдачи. Если книга не выдана никому, читатель и дата пусты. Используйте LEFT JOIN для loans.',
       10,
       'Сначала JOIN books с authors, затем LEFT JOIN loans по id_book, и LEFT JOIN readers по id_reader.',
       'SELECT b.name_book, a.surname AS author_surname, r.surname AS reader_surname, l.loan_date FROM books b JOIN authors a ON b.id_author = a.id_author LEFT JOIN loans l ON b.id_book = l.id_book LEFT JOIN readers r ON l.id_reader = r.id_reader;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 6 AND task_number = 12);


-- =========================================
-- Занятие 7. Подзапросы (вложенные запросы)
-- =========================================

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 7, 'Занятие 7. Подзапросы (вложенные запросы)', 1,
       '7.1. Скалярный подзапрос в WHERE',
       'Найдите книги, у которых количество страниц больше среднего. Выведите название и количество страниц.',
       10,
       'Сравните pages со значением, полученным из подзапроса, который вычисляет среднее количество страниц (AVG). Подзапрос должен вернуть одно число.',
       'SELECT name_book, pages FROM books WHERE pages > (SELECT AVG(pages) FROM books);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 7 AND task_number = 1);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 7, 'Занятие 7. Подзапросы (вложенные запросы)', 2,
       '7.2. Подзапрос с IN',
       'Выведите фамилии и имена авторов, у которых есть книги, изданные после 1860 года.',
       10,
       'Используйте IN с подзапросом, который возвращает id_author книг, изданных после 1860 года.',
       'SELECT surname, name FROM authors WHERE id_author IN (SELECT id_author FROM books WHERE year > 1860);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 7 AND task_number = 2);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 7, 'Занятие 7. Подзапросы (вложенные запросы)', 3,
       '7.3. Подзапрос с NOT IN',
       'Выведите фамилии и имена авторов, у которых нет книг (т.е. они не встречаются в таблице books).',
       10,
       'NOT IN с подзапросом, возвращающим id_author из books. Важно исключить NULL в подзапросе, иначе NOT IN не сработает – добавьте WHERE id_author IS NOT NULL.',
       'SELECT surname, name FROM authors WHERE id_author NOT IN (SELECT DISTINCT id_author FROM books WHERE id_author IS NOT NULL);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 7 AND task_number = 3);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 7, 'Занятие 7. Подзапросы (вложенные запросы)', 4,
       '7.4. Подзапрос с EXISTS',
       'Перепишите задачу 2, используя EXISTS вместо IN.',
       10,
       'Используйте EXISTS с коррелированным подзапросом к таблице books, проверяя, есть ли у автора книги, изданные после 1860 года.',
       'SELECT surname, name FROM authors a WHERE EXISTS (SELECT 1 FROM books b WHERE b.id_author = a.id_author AND b.year > 1860);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 7 AND task_number = 4);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 7, 'Занятие 7. Подзапросы (вложенные запросы)', 5,
       '7.5. Подзапрос с NOT EXISTS',
       'Перепишите задачу 3, используя NOT EXISTS.',
       10,
       'Используйте NOT EXISTS с коррелированным подзапросом, который проверяет наличие хотя бы одной книги у автора.',
       'SELECT surname, name FROM authors a WHERE NOT EXISTS (SELECT 1 FROM books b WHERE b.id_author = a.id_author);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 7 AND task_number = 5);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 7, 'Занятие 7. Подзапросы (вложенные запросы)', 6,
       '7.6. Подзапрос в SELECT (коррелированный)',
       'Выведите название книги, год издания и количество книг того же автора (добавьте столбец same_author_count).',
       10,
       'Добавьте в SELECT коррелированный подзапрос, который считает количество книг того же автора, что и текущая строка.',
       'SELECT name_book, year, (SELECT COUNT(*) FROM books b2 WHERE b2.id_author = b1.id_author) AS same_author_count FROM books b1;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 7 AND task_number = 6);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 7, 'Занятие 7. Подзапросы (вложенные запросы)', 7,
       '7.7. Подзапрос в FROM',
       'Найдите авторов, у которых средний год издания книг позже 1850 года. Используйте подзапрос в FROM, который группирует books, а затем внешний SELECT фильтрует по avg_year > 1850. Выведите id_author и средний год.',
       10,
       'Используйте подзапрос в FROM, который группирует книги по авторам и вычисляет средний год издания. Затем во внешнем запросе отфильтруйте группы со средним годом больше 1850 года.',
       'SELECT id_author, avg_year FROM (SELECT id_author, AVG(year) AS avg_year FROM books GROUP BY id_author) AS sub WHERE avg_year > 1850;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 7 AND task_number = 7);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 7, 'Занятие 7. Подзапросы (вложенные запросы)', 8,
       '7.8. Подзапрос с ALL',
       'Найдите книги, у которых количество страниц больше, чем у всех книг автора с id_author = 1 (Пушкин). Выведите название, количество страниц и id_author.',
       10,
       'Используйте ALL с подзапросом, который возвращает количество страниц книг указанного автора (id_author = 1). Условие отберёт книги, у которых страниц больше, чем у любой книги этого автора.',
       'SELECT name_book, pages, id_author FROM books WHERE pages > ALL (SELECT pages FROM books WHERE id_author = 1 AND pages IS NOT NULL);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 7 AND task_number = 8);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 7, 'Занятие 7. Подзапросы (вложенные запросы)', 9,
       '7.9. Подзапрос с ANY',
       'Найдите книги, у которых количество страниц больше, чем хотя бы у одной книги автора с id_author = 2 (Толстой). Выведите название, страницы и id_author.',
       10,
       'Используйте ANY с подзапросом, который возвращает количество страниц книг указанного автора (id_author = 2). Условие отберёт книги, у которых страниц больше хотя бы одной книги этого автора.',
       'SELECT name_book, pages, id_author FROM books WHERE pages > ANY (SELECT pages FROM books WHERE id_author = 2 AND pages IS NOT NULL);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 7 AND task_number = 9);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 7, 'Занятие 7. Подзапросы (вложенные запросы)', 10,
       '7.10. Вложенный подзапрос (два уровня)',
       'Найдите авторов, у которых максимальное количество страниц среди их книг больше, чем среднее количество страниц всех книг. Подсказка: сначала найдите среднее всех книг (самый глубокий подзапрос), затем найдите максимальные страницы по авторам и сравните.',
       10,
       'Сравните максимальное количество страниц книг автора (коррелированный подзапрос) со средним количеством страниц всех книг (скалярный подзапрос). Используйте > для сравнения.',
       'SELECT a.surname FROM authors a WHERE (SELECT MAX(pages) FROM books b WHERE b.id_author = a.id_author) > (SELECT AVG(pages) FROM books);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 7 AND task_number = 10);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 7, 'Занятие 7. Подзапросы (вложенные запросы)', 11,
       '7.11. Подзапрос с агрегацией и сравнением строк',
       'Выведите фамилию автора и название самой толстой книги этого автора (с максимальным pages). Используйте подзапрос для нахождения максимального pages для каждого автора.',
       10,
       'Используйте сравнение кортежа (id_author, pages) с результатом подзапроса, который для каждого автора находит максимальное количество страниц. Подзапрос должен возвращать пары (id_author, MAX(pages)).',
       'SELECT a.surname, b.name_book FROM authors a JOIN books b ON a.id_author = b.id_author WHERE (b.id_author, b.pages) IN (SELECT id_author, MAX(pages) FROM books GROUP BY id_author);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 7 AND task_number = 11);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 7, 'Занятие 7. Подзапросы (вложенные запросы)', 12,
       '7.12. Комбинация JOIN и подзапроса',
       'Выведите фамилию автора, название книги и количество книг этого автора (используйте подзапрос в SELECT или JOIN с подзапросом). Результат отсортируйте по фамилии.',
       10,
       'Добавьте в SELECT коррелированный подзапрос, который для каждого автора (из внешнего запроса) считает общее количество его книг. Результат отсортируйте по фамилии автора.',
       'SELECT a.surname, b.name_book, (SELECT COUNT(*) FROM books b2 WHERE b2.id_author = a.id_author) AS author_book_count FROM authors a JOIN books b ON a.id_author = b.id_author ORDER BY a.surname;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 7 AND task_number = 12);


-- ===============================
-- Занятие 8. Представления (VIEW)
-- ===============================

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 8, 'Занятие 8. Представления (VIEW)', 1,
       '8.1. Создание простого представления',
       'Создайте представление active_books, которое содержит все столбцы таблицы books, но только те книги, у которых количество страниц больше 100.',
       10,
       'Создайте представление, которое выбирает все столбцы из books, но только те строки, где количество страниц больше 100.',
       'CREATE VIEW active_books AS SELECT * FROM books WHERE pages > 100;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 8 AND task_number = 1);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 8, 'Занятие 8. Представления (VIEW)', 2,
       '8.2. Использование представления',
       'Напишите запрос к представлению active_books, который выводит названия книг, изданных после 1850 года.',
       10,
       'Напишите запрос к представлению, который выводит названия книг, изданных после 1850 года.',
       'SELECT name_book FROM active_books WHERE year > 1850;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 8 AND task_number = 2);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 8, 'Занятие 8. Представления (VIEW)', 3,
       '8.3. Представление с JOIN',
       'Создайте представление book_author_info, которое выводит: название книги (name_book), год издания, фамилию автора и имя автора. Используйте JOIN.',
       10,
       'Создайте представление, объединив таблицы books и authors, и выберите название книги, год издания, фамилию и имя автора.',
       'CREATE VIEW book_author_info AS SELECT b.name_book, b.year, a.surname, a.name FROM books b JOIN authors a ON b.id_author = a.id_author;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 8 AND task_number = 3);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 8, 'Занятие 8. Представления (VIEW)', 4,
       '8.4. Представление с агрегацией',
       'Создайте представление author_avg_pages, которое для каждого автора выводит id_author, фамилию и среднее количество страниц его книг (avg_pages). Округлите среднее до целого.',
       10,
       'Сгруппируйте авторов, используя LEFT JOIN, чтобы включить и тех, у кого нет книг. Вычислите среднее количество страниц (с округлением) для каждого автора.',
       'CREATE VIEW author_avg_pages AS SELECT a.id_author, a.surname, ROUND(AVG(b.pages)) AS avg_pages FROM authors a LEFT JOIN books b ON a.id_author = b.id_author GROUP BY a.id_author, a.surname;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 8 AND task_number = 4);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 8, 'Занятие 8. Представления (VIEW)', 5,
       '8.5. Замена представления',
       'Измените представление active_books, добавив условие pages IS NOT NULL (т.е. книги без указания страниц не попадают). Используйте CREATE OR REPLACE VIEW.',
       10,
       'Замените представление, добавив в условие также проверку, что pages не равно NULL.',
       'CREATE OR REPLACE VIEW active_books AS SELECT * FROM books WHERE pages > 100 AND pages IS NOT NULL;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 8 AND task_number = 5);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 8, 'Занятие 8. Представления (VIEW)', 6,
       '8.6. Удаление представления',
       'Напишите команду для удаления представления active_books.',
       10,
       'Используйте DROP VIEW с именем представления.',
       'DROP VIEW active_books;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 8 AND task_number = 6);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 8, 'Занятие 8. Представления (VIEW)', 7,
       '8.7. Представление на основе представления',
       'Сначала создайте представление books_after_1850, которое содержит книги, изданные после 1850 года (все столбцы). Затем создайте представление long_books_after_1850, которое на основе первого представления выбирает книги с количеством страниц больше 500. Выполните выборку из второго представления.',
       10,
       'Создайте первое представление с фильтром по году. Затем создайте второе представление как SELECT из первого с дополнительным условием по страницам. Выполните SELECT из второго.',
       'CREATE VIEW books_after_1850 AS SELECT * FROM books WHERE year > 1850; CREATE VIEW long_books_after_1850 AS SELECT * FROM books_after_1850 WHERE pages > 500; SELECT * FROM long_books_after_1850;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 8 AND task_number = 7);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 8, 'Занятие 8. Представления (VIEW)', 8,
       '8.8. Материализованное представление (ознакомительное)',
       'Создайте материализованное представление mv_author_stats, которое для каждого автора выводит фамилию, максимальное количество страниц среди его книг и минимальный год издания. Напишите команду для обновления этого представления.',
       10,
       'Создайте материализованное представление с группировкой по автору, выбрав фамилию, максимум страниц и минимум года. После создания при необходимости обновляйте его специальной командой.',
       'CREATE MATERIALIZED VIEW mv_author_stats AS SELECT a.surname, MAX(b.pages) AS max_pages, MIN(b.year) AS min_year FROM authors a LEFT JOIN books b ON a.id_author = b.id_author GROUP BY a.id_author, a.surname; REFRESH MATERIALIZED VIEW mv_author_stats;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 8 AND task_number = 8);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 8, 'Занятие 8. Представления (VIEW)', 9,
       '8.9. Практическое использование представления для отчёта',
       'Создайте представление reader_loan_stats, которое для каждого читателя (из таблицы readers) выводит его фамилию и количество взятых книг (используя таблицу loans). Если читатель не брал книг, количество должно быть 0.',
       10,
       'Соедините readers с loans через LEFT JOIN, сгруппируйте по читателю, посчитайте количество выдач (COUNT(l.id_loan)).',
       'CREATE VIEW reader_loan_stats AS SELECT r.surname, COUNT(l.id_loan) AS loan_count FROM readers r LEFT JOIN loans l ON r.id_reader = l.id_reader GROUP BY r.id_reader, r.surname;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 8 AND task_number = 9);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 8, 'Занятие 8. Представления (VIEW)', 10,
       '8.10. Представление с вычисляемым столбцом',
       'Создайте представление books_with_price_estimate, которое добавляет к таблице books вычисляемый столбец estimated_price по формуле:
    - если pages < 100, то цена 200 рублей;
    - если pages от 100 до 300, то 500 рублей;
    - если pages > 300, то 800 рублей.
Выведите все данные из этого представления.',
       10,
       'Используйте CASE WHEN для создания вычисляемого столбца с ценой в зависимости от количества страниц. После создания представления выберите все данные из него.',
       'CREATE VIEW books_with_price_estimate AS SELECT *, CASE WHEN pages < 100 THEN 200 WHEN pages BETWEEN 100 AND 300 THEN 500 ELSE 800 END AS estimated_price FROM books; SELECT * FROM books_with_price_estimate;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 8 AND task_number = 10);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 8, 'Занятие 8. Представления (VIEW)', 11,
       '8.11. Фильтрация через представление',
       'Создайте представление authors_with_books, которое содержит только тех авторов, у которых есть хотя бы одна книга. Используйте EXISTS. Затем сделайте выборку из этого представления, отсортировав авторов по фамилии.',
       10,
       'Создайте представление, которое оставляет только тех авторов, у которых есть хотя бы одна книга (используйте EXISTS). Затем выполните выборку с сортировкой по фамилии.',
       'CREATE VIEW authors_with_books AS SELECT * FROM authors a WHERE EXISTS (SELECT 1 FROM books b WHERE b.id_author = a.id_author); SELECT * FROM authors_with_books ORDER BY surname;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 8 AND task_number = 11);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 8, 'Занятие 8. Представления (VIEW)', 12,
       '8.12. Комплексное задание',
       'Создайте представление full_library_info, которое объединяет информацию о книгах, авторах и читателях (через loans). Представление должно содержать: название книги, фамилию автора, фамилию читателя, дату выдачи. Если книга не выдана, читатель и дата должны быть NULL. Выполните выборку из представления, ограничив её 10 записями.',
       10,
       'Соедините books с authors (INNER JOIN), затем LEFT JOIN loans, затем LEFT JOIN readers. Выберите нужные столбцы. В конце выполните SELECT с LIMIT 10.',
       'CREATE VIEW full_library_info AS SELECT b.name_book, a.surname AS author_surname, r.surname AS reader_surname, l.loan_date FROM books b JOIN authors a ON b.id_author = a.id_author LEFT JOIN loans l ON b.id_book = l.id_book LEFT JOIN readers r ON l.id_reader = r.id_reader; SELECT * FROM full_library_info LIMIT 10;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 8 AND task_number = 12);


-- =========================================
-- Занятие 9. Индексы и оптимизация запросов
-- =========================================

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 9, 'Занятие 9. Индексы и оптимизация запросов', 1,
       '9.1. Создание простого индекса',
       'Создайте индекс с именем idx_books_pages на столбце pages таблицы books.',
       10,
       'Используйте команду CREATE INDEX, указав имя индекса, таблицу и столбец, на котором нужно построить индекс.',
       'CREATE INDEX idx_books_pages ON books(pages);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 9 AND task_number = 1);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 9, 'Занятие 9. Индексы и оптимизация запросов', 2,
       '9.2. Составной индекс',
       'Создайте составной индекс idx_books_author_year_desc на таблице books по столбцу id_author (по возрастанию) и year (по убыванию).',
       10,
       'Создайте составной индекс: сначала id_author по возрастанию, затем year по убыванию. Укажите направление для каждого столбца.',
       'CREATE INDEX idx_books_author_year_desc ON books(id_author ASC, year DESC);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 9 AND task_number = 2);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 9, 'Занятие 9. Индексы и оптимизация запросов', 3,
       '9.3. Индекс на внешний ключ',
       'Создайте индекс на столбце id_author таблицы books. Объясните, для какой операции этот индекс полезен.',
       10,
       'Индекс ускоряет соединение таблиц (JOIN) и каскадные операции (ON DELETE CASCADE). Напишите CREATE INDEX, затем текстовое пояснение.',
       'CREATE INDEX idx_books_id_author ON books(id_author);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 9 AND task_number = 3);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 9, 'Занятие 9. Индексы и оптимизация запросов', 4,
       '9.4. Уникальный индекс',
       'Добавьте уникальный индекс на столбце surname таблицы authors. Будет ли он полезен? В каких случаях может возникнуть ошибка?',
       10,
       'Используйте CREATE UNIQUE INDEX. Подумайте: уникальность фамилий полезна для поиска, но может вызвать ошибку при попытке вставить дубликат.',
       'CREATE UNIQUE INDEX idx_unique_surname ON authors(surname);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 9 AND task_number = 4);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 9, 'Занятие 9. Индексы и оптимизация запросов', 5,
       '9.5. Удаление индекса',
       'Напишите команду для удаления индекса idx_books_pages.',
       10,
       'Используйте DROP INDEX с указанием имени индекса.',
       'DROP INDEX idx_books_pages;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 9 AND task_number = 5);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 9, 'Занятие 9. Индексы и оптимизация запросов', 6,
       '9.6. Индекс на выражение',
       'Создайте индекс idx_books_upper_name на выражении UPPER(name_book). Для какого запроса он ускорит поиск? Напишите этот запрос.',
       10,
       'Создайте индекс на выражении UPPER(name_book). Напишите запрос, который будет использовать этот индекс (условие с UPPER).',
       'CREATE INDEX idx_books_upper_name ON books(UPPER(name_book)); SELECT * FROM books WHERE UPPER(name_book) = ''ВОЙНА И МИР'';'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 9 AND task_number = 6);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 9, 'Занятие 9. Индексы и оптимизация запросов', 7,
       '9.7. Частичный индекс',
       'Создайте частичный индекс idx_recent_books для книг, изданных после 1900 года (только для этих строк) по столбцу year.',
       10,
       'Создайте частичный индекс, включающий только строки, где year > 1900. Укажите условие в CREATE INDEX.',
       'CREATE INDEX idx_recent_books ON books(year) WHERE year > 1900;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 9 AND task_number = 7);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 9, 'Занятие 9. Индексы и оптимизация запросов', 8,
       '9.8. Просмотр информации об индексах',
       'Напишите запрос к системному каталогу pg_indexes, который выводит все индексы таблицы books.',
       10,
       'Системное представление pg_indexes содержит столбцы schemaname, tablename, indexname, indexdef. Используйте WHERE tablename = ''books''.',
       'SELECT * FROM pg_indexes WHERE tablename = ''books'';'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 9 AND task_number = 8);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 9, 'Занятие 9. Индексы и оптимизация запросов', 9,
       '9.9. Оценка эффективности',
       'У вас есть таблица books с миллионом записей. Какой индекс вы создадите для ускорения запроса: SELECT * FROM books WHERE name_book = ''Война и мир'';',
       10,
       'Какой индекс ускорит поиск по точному совпадению названия книги? Укажите тип индекса и столбец.',
       'CREATE INDEX idx_books_name ON books(name_book);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 9 AND task_number = 9);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 9, 'Занятие 9. Индексы и оптимизация запросов', 10,
       '9.10. Практическое задание со звёздочкой',
       'Создайте индекс для ускорения следующего запроса: SELECT id_author, COUNT(*) FROM books WHERE year > 1850 GROUP BY id_author; (Подсказка: подумайте о составном индексе, который покрывает и фильтрацию, и группировку.)',
       10,
       'Создайте составной индекс, который поможет фильтровать по year и группировать по id_author. Подумайте, какой столбец должен быть первым для эффективной фильтрации.',
       'CREATE INDEX idx_books_year_author ON books(year, id_author);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 9 AND task_number = 10);


-- ================================================
-- Занятие 10. Транзакции (BEGIN, COMMIT, ROLLBACK)
-- ================================================

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 10, 'Занятие 10. Транзакции (BEGIN, COMMIT, ROLLBACK)', 1,
       '10.1. Простая транзакция с COMMIT',
       'Напишите транзакцию, которая добавляет нового автора «Булгаков Михаил Афанасьевич» (год рождения 1891, год смерти 1940) и его книгу «Мастер и Маргарита» (год 1966, страниц 480). Оба действия должны быть в одной транзакции. Завершите транзакцию фиксацией.',
       10,
       'Начните транзакцию, вставьте автора, затем вставьте книгу (используя подзапрос для получения id_author автора). Завершите транзакцию фиксацией.',
       'BEGIN; INSERT INTO authors (surname, name, patronymic, birth, death) VALUES (''Булгаков'', ''Михаил'', ''Афанасьевич'', ''1891-05-15'', ''1940-03-10''); INSERT INTO books (name_book, id_author, year, pages) VALUES (''Мастер и Маргарита'', (SELECT id_author FROM authors WHERE surname = ''Булгаков''), 1966, 480); COMMIT;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 10 AND task_number = 1);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 10, 'Занятие 10. Транзакции (BEGIN, COMMIT, ROLLBACK)', 2,
       '10.2. Транзакция с ROLLBACK',
       'Напишите транзакцию, которая удаляет все книги, изданные до 1800 года, а затем откатывает это удаление. Убедитесь, что после отката книги остались на месте.',
       10,
       'Начните транзакцию, удалите все книги, изданные до 1800 года, затем откатите изменения. COMMIT не нужен.',
       'BEGIN; DELETE FROM books WHERE year < 1800; ROLLBACK;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 10 AND task_number = 2);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 10, 'Занятие 10. Транзакции (BEGIN, COMMIT, ROLLBACK)', 3,
       '10.3. Транзакция с двумя обновлениями',
       'Напишите транзакцию, которая:
        увеличивает количество страниц у всех книг Толстого (id_author = 2) на 50;
        уменьшает год издания всех книг Пушкина (id_author = 1) на 1.
    Затем зафиксируйте изменения.',
       10,
       'В транзакции выполните два UPDATE: для id_author = 2 увеличьте pages на 50, для id_author = 1 уменьшите year на 1. Затем зафиксируйте.',
       'BEGIN; UPDATE books SET pages = pages + 50 WHERE id_author = 2; UPDATE books SET year = year - 1 WHERE id_author = 1; COMMIT;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 10 AND task_number = 3);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 10, 'Занятие 10. Транзакции (BEGIN, COMMIT, ROLLBACK)', 4,
       '10.4. Использование SAVEPOINT',
       'Напишите транзакцию, которая:
    - вставляет нового читателя «Лебедев Сергей» (дата рождения 1985-10-10);
    - устанавливает точку сохранения;
    - пытается выдать этому читателю книгу с id_book = 999 (заведомо несуществующую, что вызовет ошибку внешнего ключа);
    - откатывается до точки сохранения; фиксирует транзакцию.
В результате читатель должен сохраниться, а ошибочная выдача – нет.',
       10,
       'Начните транзакцию, добавьте читателя, установите точку сохранения, попытайтесь выдать книгу с несуществующим id_book, откатитесь к точке сохранения, зафиксируйте.',
       'BEGIN; INSERT INTO readers (surname, name, patronymic, birth) VALUES (''Лебедев'', ''Сергей'', NULL, ''1985-10-10''); SAVEPOINT sp; INSERT INTO loans (id_book, id_reader, loan_date) VALUES (999, (SELECT id_reader FROM readers WHERE surname = ''Лебедев''), CURRENT_DATE); ROLLBACK TO sp; COMMIT;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 10 AND task_number = 4);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 10, 'Занятие 10. Транзакции (BEGIN, COMMIT, ROLLBACK)', 5,
       '10.5. Проверка атомарности',
       'Напишите транзакцию, которая пытается обновить pages у книги с id_book = 1 и одновременно вставить книгу с id_author = 999 (несуществующий автор). Транзакция должна завершиться ошибкой и откатиться. Зафиксируйте, что первое изменение (pages) не применилось.',
       10,
       'Начните транзакцию, обновите pages у одной книги и попробуйте вставить книгу с несуществующим id_author. Ошибка внешнего ключа вызовет откат всей транзакции, и первое изменение не применится.',
       'BEGIN; UPDATE books SET pages = 100 WHERE id_book = 1; INSERT INTO books (name_book, id_author) VALUES (''Тест'', 999); COMMIT;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 10 AND task_number = 5);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 10, 'Занятие 10. Транзакции (BEGIN, COMMIT, ROLLBACK)', 6,
       '10.6. Вложенные транзакции через SAVEPOINT',
       'Напишите транзакцию, которая:
    - вставляет автора «Грибоедов»;
    - устанавливает точку сохранения sp1;
    - вставляет его книгу «Горе от ума»;
    - устанавливает точку сохранения sp2;
    - обновляет год издания этой книги на 1824;
    - откатывается до sp1 (т.е. отменяет и книгу, и обновление);
    - фиксирует транзакцию.
Какие изменения останутся в базе?',
       10,
       'Напишите транзакцию: добавьте автора, установите точку сохранения, добавьте книгу, установите вторую точку, обновите год книги, откатитесь до первой точки, зафиксируйте. В результате останется только автор.',
       'BEGIN; INSERT INTO authors (surname, name) VALUES (''Грибоедов'', ''Александр''); SAVEPOINT sp1; INSERT INTO books (name_book, id_author, year) VALUES (''Горе от ума'', (SELECT id_author FROM authors WHERE surname = ''Грибоедов''), 1823); SAVEPOINT sp2; UPDATE books SET year = 1824 WHERE name_book = ''Горе от ума''; ROLLBACK TO sp1; COMMIT;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 10 AND task_number = 8);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 10, 'Занятие 10. Транзакции (BEGIN, COMMIT, ROLLBACK)', 7,
       '10.7. DDL в транзакции',
       'Напишите транзакцию, которая создаёт новую таблицу temp_log с одним столбцом msg TEXT, вставляет в неё строку ''Тест'', а затем откатывает транзакцию. Существует ли таблица temp_log после отката?',
       10,
       'Начните транзакцию, создайте таблицу, вставьте строку, затем откатите транзакцию. После отката таблица не должна существовать.',
       'BEGIN; CREATE TABLE temp_log (msg TEXT); INSERT INTO temp_log VALUES (''Тест''); ROLLBACK;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 10 AND task_number = 9);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 10, 'Занятие 10. Транзакции (BEGIN, COMMIT, ROLLBACK)', 8,
       '10.8. Практическое задание на осмысление',
       'В библиотеке есть правило: нельзя удалить автора, если у него есть книги. Однако из-за ON DELETE CASCADE это правило нарушается. Напишите транзакцию, которая временно удаляет внешний ключ с CASCADE, удаляет автора (например, Пушкина) и все его книги, а затем восстанавливает внешний ключ. В реальной практике так не делают, но для понимания транзакций полезно.',
       10,
       'В транзакции удалите ограничение внешнего ключа из таблицы books, удалите автора с id_author = 1, затем добавьте ограничение заново.',
       'BEGIN; ALTER TABLE books DROP CONSTRAINT books_id_author_fkey; DELETE FROM authors WHERE id_author = 1; ALTER TABLE books ADD CONSTRAINT books_id_author_fkey FOREIGN KEY (id_author) REFERENCES authors(id_author) ON DELETE CASCADE; COMMIT;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 10 AND task_number = 10);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 10, 'Занятие 10. Транзакции (BEGIN, COMMIT, ROLLBACK)', 9,
       '10.9. Мониторинг транзакций',
       'Напишите запрос к pg_stat_activity, который показывает все активные транзакции (где xact_start не NULL), их pid и время начала.',
       10,
       'Напишите SELECT к системному представлению pg_stat_activity, отфильтровав строки, где время начала транзакции (xact_start) не равно NULL. Выведите pid и xact_start.',
       'SELECT pid, xact_start FROM pg_stat_activity WHERE xact_start IS NOT NULL;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 10 AND task_number = 11);


-- =====================================================
-- Занятие 11. Итоговый проект: «Библиотечная аналитика»
-- =====================================================

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 11, 'Занятие 11. Итоговый проект: «Библиотечная аналитика»', 1,
       '11.1. Создание таблицы book_stats',
       'Создайте таблицу book_stats для хранения статистики по книгам:
    - id_book INT PRIMARY KEY, ссылается на books(id_book) с ON DELETE CASCADE;
    - times_loaned INT DEFAULT 0;
    - last_loan_date DATE (может быть NULL).',
       1,
       'Создайте таблицу с внешним ключом на books, первичным ключом, счётчиком выдач (по умолчанию 0) и датой последней выдачи (может быть NULL).',
       'CREATE TABLE book_stats (id_book INT PRIMARY KEY REFERENCES books(id_book) ON DELETE CASCADE, times_loaned INT DEFAULT 0, last_loan_date DATE);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 11 AND task_number = 1);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 11, 'Занятие 11. Итоговый проект: «Библиотечная аналитика»', 2,
       '11.2. Заполнение book_stats на основе loans',
       'Заполните таблицу book_stats на основе данных из loans:
    - times_loaned = количество выдач этой книги,
    - last_loan_date = максимальная дата выдачи (если выдач не было – NULL).
Используйте INSERT INTO ... SELECT ... GROUP BY.',
       1,
       'Используйте INSERT INTO ... SELECT, сгруппировав по книге. Для каждой книги посчитайте количество выдач и максимальную дату выдачи. Книги, которые не выдавались, не попадут в результат (это нормально).',
       'INSERT INTO book_stats (id_book, times_loaned, last_loan_date) SELECT id_book, COUNT(*), MAX(loan_date) FROM loans GROUP BY id_book;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 11 AND task_number = 2);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 11, 'Занятие 11. Итоговый проект: «Библиотечная аналитика»', 3,
       '11.3. Добавление столбца total_loans в books и обновление',
       'Добавьте в таблицу books столбец total_loans типа INT DEFAULT 0. Затем обновите этот столбец, установив значения из book_stats.times_loaned (с помощью UPDATE ... FROM или подзапроса).',
       1,
       'Добавьте столбец total_loans в таблицу books со значением по умолчанию 0. Затем обновите его значениями из book_stats, соединив таблицы по id_book.',
       'ALTER TABLE books ADD COLUMN total_loans INT DEFAULT 0; UPDATE books SET total_loans = bs.times_loaned FROM book_stats bs WHERE books.id_book = bs.id_book;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 11 AND task_number = 3);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 11, 'Занятие 11. Итоговый проект: «Библиотечная аналитика»', 4,
       '11.4. Книги, которые никогда не выдавались',
       'Выведите список книг, которые никогда не выдавались (total_loans = 0). Включите: название книги, фамилию автора, год издания. Отсортируйте по году (от старых к новым).',
       1,
       'Соедините books с authors, отфильтруйте книги с total_loans = 0 (никогда не выдавались). Отсортируйте по году издания.',
       'SELECT b.name_book, a.surname, b.year FROM books b JOIN authors a ON b.id_author = a.id_author WHERE b.total_loans = 0 ORDER BY b.year;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 11 AND task_number = 4);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 11, 'Занятие 11. Итоговый проект: «Библиотечная аналитика»', 5,
       '11.5. Статистика по авторам',
       'Для каждого автора выведите: фамилию, общее количество его книг, общее количество выдач всех его книг (сумма total_loans), среднее количество страниц по его книгам (округлить до целого). Отсортируйте по убыванию общего количества выдач. Используйте LEFT JOIN и GROUP BY.',
       1,
       'Соедините authors с books через LEFT JOIN, сгруппируйте по id_author и фамилии. Для количества книг – COUNT(b.id_book). Для суммы выдач – SUM(COALESCE(b.total_loans,0)). Для среднего – ROUND(AVG(b.pages)).',
       'SELECT a.surname, COUNT(b.id_book) AS book_count, SUM(COALESCE(b.total_loans,0)) AS total_loans_sum, ROUND(AVG(b.pages)) AS avg_pages FROM authors a LEFT JOIN books b ON a.id_author = b.id_author GROUP BY a.id_author, a.surname ORDER BY total_loans_sum DESC;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 11 AND task_number = 5);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 11, 'Занятие 11. Итоговый проект: «Библиотечная аналитика»', 6,
       '11.6. Читатель, взявший больше всего книг',
       'Найдите читателя (фамилию, имя), который взял больше всего книг (по количеству записей в loans). Если таких несколько – выведите любого. Используйте подзапрос или LIMIT.',
       1,
       'Соедините readers с loans, сгруппируйте по читателю, посчитайте количество выдач, отсортируйте по убыванию и ограничьте 1 записью.',
       'SELECT r.surname, r.name FROM readers r JOIN loans l ON r.id_reader = l.id_reader GROUP BY r.id_reader, r.surname, r.name ORDER BY COUNT(*) DESC LIMIT 1;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 11 AND task_number = 6);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 11, 'Занятие 11. Итоговый проект: «Библиотечная аналитика»', 7,
       '11.7. Представление active_loans и запрос к нему',
       'Создайте представление active_loans, которое показывает:
    - фамилию читателя,
    - название книги,
    - дату выдачи для всех активных выдач (return_date IS NULL).
Затем напишите запрос к этому представлению, чтобы вывести список активных выдач, отсортированный по дате выдачи (сначала самые старые).',
       1,
       'Создайте представление, которое соединяет loans, readers и books, выбирает фамилию читателя, название книги и дату выдачи для активных выдач (return_date IS NULL). Затем выполните выборку из представления, отсортировав по дате выдачи.',
       'CREATE VIEW active_loans AS SELECT r.surname, b.name_book, l.loan_date FROM loans l JOIN readers r ON l.id_reader = r.id_reader JOIN books b ON l.id_book = b.id_book WHERE l.return_date IS NULL; SELECT * FROM active_loans ORDER BY loan_date;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 11 AND task_number = 7);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 11, 'Занятие 11. Итоговый проект: «Библиотечная аналитика»', 8,
       '11.8. Дни с последней выдачи',
       'Напишите запрос, который для каждой книги выводит:
    - название,
    - количество дней, прошедших с последней выдачи (CURRENT_DATE - last_loan_date).
Если книга никогда не выдавалась – выведите NULL.
Используйте JOIN с book_stats.',
       1,
       'Соедините books с book_stats (LEFT JOIN). Для каждой книги выведите её название и разницу между текущей датой и last_loan_date. Если книга не выдавалась, last_loan_date будет NULL, разница тоже будет NULL.',
       'SELECT b.name_book, CURRENT_DATE - bs.last_loan_date AS days_since_last_loan FROM books b LEFT JOIN book_stats bs ON b.id_book = bs.id_book;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 11 AND task_number = 8);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 11, 'Занятие 11. Итоговый проект: «Библиотечная аналитика»', 9,
       '11.9. Индекс на loan_date',
       'Создайте такой индекс на столбце loan_date таблицы loans, чтобы он ускорил выполнение запроса: SELECT * FROM loans WHERE loan_date BETWEEN ''2025-01-01'' AND ''2025-12-31'';',
       1,
       'Используйте CREATE INDEX.',
       'CREATE INDEX idx_loans_loan_date ON loans(loan_date);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 11 AND task_number = 9);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 11, 'Занятие 11. Итоговый проект: «Библиотечная аналитика»', 10,
       '11.10. Составной индекс и пример запроса',
       'Создайте составной индекс на таблице books по полям id_author и year.',
       1,
       'Создайте составной индекс на таблице books по полям id_author и year.',
       'CREATE INDEX idx_books_author_year ON books(id_author, year);'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 11 AND task_number = 10);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 11, 'Занятие 11. Итоговый проект: «Библиотечная аналитика»', 11,
       '11.11. Транзакция: добавление читателя и выдача книги',
       'Напишите транзакцию, которая:
    - добавляет нового читателя «Смирнов Олег Иванович»,
    - дата рождения 1992-04-10;
    - выдаёт ему книгу с id_book = 3 (сегодняшняя дата);
    - фиксирует изменения.
После выполнения проверьте (мысленно или в песочнице), что читатель появился в readers, а в loans – соответствующая запись.',
       1,
       'Начните транзакцию, добавьте читателя, затем выдайте ему книгу (используйте подзапрос для получения id_reader). Зафиксируйте изменения.',
       'BEGIN; INSERT INTO readers (surname, name, patronymic, birth) VALUES (''Смирнов'', ''Олег'', ''Иванович'', ''1992-04-10''); INSERT INTO loans (id_book, id_reader, loan_date) VALUES (3, (SELECT id_reader FROM readers WHERE surname = ''Смирнов'' AND name = ''Олег''), CURRENT_DATE); COMMIT;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 11 AND task_number = 11);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 11, 'Занятие 11. Итоговый проект: «Библиотечная аналитика»', 12,
       '11.12. Транзакция с точкой сохранения',
       'Напишите транзакцию с точкой сохранения:
    - начать транзакцию;
    - обновить total_loans для книги с id_book = 1, установив значение 100;
    - создать точку сохранения sp1;
    - обновить total_loans для книги с id_book = 2, установив значение 200;
    - откатиться к sp1; зафиксировать транзакцию.
В результате изменения должны затронуть только книгу 1, а книга 2 остаться без изменений.',
       1,
       'Начните транзакцию, обновите total_loans для книги 1, создайте точку сохранения, обновите total_loans для книги 2, затем откатитесь до точки сохранения и зафиксируйте транзакцию.',
       'BEGIN; UPDATE books SET total_loans = 100 WHERE id_book = 1; SAVEPOINT sp1; UPDATE books SET total_loans = 200 WHERE id_book = 2; ROLLBACK TO sp1; COMMIT;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 11 AND task_number = 12);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'METHODOLOGY', 11, 'Занятие 11. Итоговый проект: «Библиотечная аналитика»', 13,
       '11.13. Итоговый отчёт',
       'Составьте один запрос (без CTE и оконных функций), который выводит:
    - общее количество книг,
    - общее количество читателей,
    - количество активных выдач (где return_date IS NULL),
    - общее количество выдач за всё время,
    - среднее количество страниц по всем книгам (округлить до целого).
Названия столбцов: total_books, total_readers, active_loans, all_time_loans, avg_pages.',
       1,
       'Используйте скалярные подзапросы в одном SELECT: каждый подзапрос возвращает одно значение. Вычислите общее количество книг, читателей, активных выдач, всех выдач и среднее страниц (с округлением).',
       'SELECT (SELECT COUNT(*) FROM books) AS total_books, (SELECT COUNT(*) FROM readers) AS total_readers, (SELECT COUNT(*) FROM loans WHERE return_date IS NULL) AS active_loans, (SELECT COUNT(*) FROM loans) AS all_time_loans, (SELECT ROUND(AVG(pages)) FROM books) AS avg_pages;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'METHODOLOGY' AND lesson_number = 11 AND task_number = 13);