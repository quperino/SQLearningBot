-- ******************************************
-- СЕКЦИЯ ПОВЫШЕННЫЙ УРОВЕНЬ (ADVANCED) *****
-- ******************************************

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'ADVANCED', 1, 'Повышенный уровень', 1,
       '1. Рекурсивный обобщённый запрос (CTE) для иерархии категорий',
       'Новая теория: обобщённые табличные выражения (CTE) с рекурсией (WITH RECURSIVE). Рекурсивный CTE состоит из базовой части (не рекурсивной) и рекурсивной части, соединённых UNION ALL. Позволяет обходить иерархические структуры (деревья, графы).

Задача: Добавьте в базу данных таблицу `categories` для хранения иерархии книжных жанров:
    - id SERIAL PRIMARY KEY
    - name VARCHAR(100) NOT NULL
    - parent_id INT REFERENCES categories(id) ON DELETE CASCADE

Наполните её данными (не менее 5 жанров, 2-3 уровня вложенности). Напишите рекурсивный CTE, который выводит все подкатегории для корневой категории с именем «Художественная литература» (или любой другой корень). Результат должен содержать: id, name, parent_id, уровень вложенности (level). Отсортируйте по уровню, затем по имени.',
       15,
       'Создайте таблицу categories, вставьте тестовые данные. Для рекурсивного CTE используйте WITH RECURSIVE. В базовой части выберите корневую категорию (parent_id IS NULL). В рекурсивной части присоедините дочерние категории через JOIN. Добавьте счётчик уровня.',
       'CREATE TABLE categories (id SERIAL PRIMARY KEY, name VARCHAR(100) NOT NULL, parent_id INT REFERENCES categories(id) ON DELETE CASCADE);
INSERT INTO categories (name, parent_id) VALUES (''Художественная литература'', NULL), (''Русская классика'', 1), (''Зарубежная классика'', 1), (''Проза'', 2), (''Поэзия'', 2);
WITH RECURSIVE cat_tree AS (SELECT id, name, parent_id, 1 AS level FROM categories WHERE name = ''Художественная литература'' UNION ALL SELECT c.id, c.name, c.parent_id, ct.level + 1 FROM categories c JOIN cat_tree ct ON c.parent_id = ct.id) SELECT * FROM cat_tree ORDER BY level, name;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'ADVANCED' AND lesson_number = 1 AND task_number = 1);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'ADVANCED', 1, 'Повышенный уровень', 2,
       '2. Оконные функции для ранжирования и накопительных итогов',
       'Новая теория: оконные функции (ROW_NUMBER(), RANK(), SUM(...) OVER(...)). Они вычисляют значения по набору строк, связанных с текущей строкой, не сворачивая результат. PARTITION BY делит строки на группы, ORDER BY задаёт порядок внутри группы.

Задача: Используя таблицы `books` и `authors`, напишите запрос, который для каждого автора выводит:
    - название книги,
    - год издания,
    - порядковый номер книги по году (ROW_NUMBER, начиная с самой старой),
    - ранг книги по году (RANK, учитывающий совпадения),
    - накопительную сумму страниц всех книг этого автора (от самой старой к самой новой).
Отсортируйте результат по фамилии автора и году издания.',
       15,
       'Используйте оконные функции с PARTITION BY id_author. ROW_NUMBER() и RANK() упорядочите по year ASC. Для накопительной суммы примените SUM(pages) OVER (PARTITION BY id_author ORDER BY year). Не забудьте JOIN books с authors.',
       'SELECT a.surname, b.name_book, b.year, ROW_NUMBER() OVER (PARTITION BY a.id_author ORDER BY b.year) AS book_num, RANK() OVER (PARTITION BY a.id_author ORDER BY b.year) AS book_rank, SUM(b.pages) OVER (PARTITION BY a.id_author ORDER BY b.year) AS cumulative_pages FROM authors a JOIN books b ON a.id_author = b.id_author ORDER BY a.surname, b.year;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'ADVANCED' AND lesson_number = 1 AND task_number = 2);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'ADVANCED', 1, 'Повышенный уровень', 3,
       '3. Хранимая функция для расчёта штрафа за просрочку',
       'Новая теория: хранимые функции на PL/pgSQL. Синтаксис: CREATE FUNCTION имя(параметры) RETURNS тип LANGUAGE plpgsql AS $$ ... $$. Можно использовать переменные, условные операторы (IF), циклы.

Задача: Создайте функцию `calculate_fine(reader_id INT, book_id INT)`, которая возвращает штраф (в рублях) за просрочку возврата книги. Правила:
    - Штраф начисляется только если книга не возвращена (return_date IS NULL) И текущая дата (CURRENT_DATE) позже ожидаемой даты возврата (loan_date + 14 дней).
    - Штраф = 10 рублей за каждый день просрочки, но не более 500 рублей.
    - Если книга возвращена или просрочки нет, функция возвращает 0.
    - Используйте таблицы `loans`, `books`, `readers`.
    - Если переданные reader_id и book_id не соответствуют активной выдаче, возвращайте -1 (ошибка).
После создания функции выполните запрос, который для каждого активного должника выводит фамилию, название книги, сумму штрафа, используя эту функцию.',
       15,
       'Функция должна проверять существование активной выдачи (return_date IS NULL). Вычисляйте количество дней просрочки: CURRENT_DATE - (loan_date + interval ''14 days''). Используйте IF для проверок. В основном запросе примените функцию для каждой строки.',
       'CREATE OR REPLACE FUNCTION calculate_fine(reader_id INT, book_id INT) RETURNS INTEGER LANGUAGE plpgsql AS $$ DECLARE loan_date DATE; days_overdue INT; fine INT := 0; BEGIN SELECT l.loan_date INTO loan_date FROM loans l WHERE l.id_reader = reader_id AND l.id_book = book_id AND l.return_date IS NULL; IF NOT FOUND THEN RETURN -1; END IF; days_overdue := CURRENT_DATE - (loan_date + 14); IF days_overdue > 0 THEN fine := LEAST(days_overdue * 10, 500); END IF; RETURN fine; END; $$; SELECT r.surname, b.name_book, calculate_fine(r.id_reader, b.id_book) AS fine FROM loans l JOIN readers r ON l.id_reader = r.id_reader JOIN books b ON l.id_book = b.id_book WHERE l.return_date IS NULL AND calculate_fine(r.id_reader, b.id_book) > 0;'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'ADVANCED' AND lesson_number = 1 AND task_number = 3);

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql)
SELECT 'ADVANCED', 1, 'Повышенный уровень', 4,
       '4. Триггер для автоматического обновления статистики',
       'Новая теория: триггеры (BEFORE/AFTER INSERT, UPDATE, DELETE). Триггерная функция возвращает тип TRIGGER, использует переменные NEW и OLD. Триггер создаётся командой CREATE TRIGGER.

Задача: У нас есть таблица `book_stats` (создаётся в итоговом проекте). Создайте триггер, который автоматически обновляет `times_loaned` и `last_loan_date` в `book_stats` после каждой новой выдачи книги (INSERT в таблицу `loans`). Триггер должен:
    - увеличить `times_loaned` на 1 для соответствующей книги,
    - установить `last_loan_date` равным дате выдачи.
    - Если записи для книги ещё нет в `book_stats`, создать её.
После создания триггера выполните INSERT новой выдачи и проверьте, что статистика обновилась.',
       15,
       'Создайте триггерную функцию на PL/pgSQL, используя INSERT ... ON CONFLICT DO UPDATE. Затем создайте триггер AFTER INSERT ON loans. Для проверки вставьте новую выдачу и сделайте SELECT из book_stats.',
       'CREATE OR REPLACE FUNCTION update_book_stats() RETURNS TRIGGER LANGUAGE plpgsql AS $$ BEGIN INSERT INTO book_stats (id_book, times_loaned, last_loan_date) VALUES (NEW.id_book, 1, NEW.loan_date) ON CONFLICT (id_book) DO UPDATE SET times_loaned = book_stats.times_loaned + 1, last_loan_date = NEW.loan_date; RETURN NEW; END; $$; CREATE TRIGGER trg_update_book_stats AFTER INSERT ON loans FOR EACH ROW EXECUTE FUNCTION update_book_stats();'
WHERE NOT EXISTS (SELECT 1 FROM tasks WHERE section = 'ADVANCED' AND lesson_number = 1 AND task_number = 4);