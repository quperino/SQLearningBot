-- =================================================================
-- Тестовые данные для in-memory H2 (загружаются при запуске тестов)
-- =================================================================

-- Несколько задач из методички (занятие 1)
INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql) VALUES
('METHODOLOGY', 1, 'Занятие 1. Создание таблиц', 1, '1.1. Создание таблицы publishers',
 'Создайте таблицу publishers с полями: id_publisher SERIAL PRIMARY KEY, name VARCHAR(200) NOT NULL, city VARCHAR(100), founded_year INT.',
 5, 'Используйте CREATE TABLE', 'CREATE TABLE publishers (id_publisher SERIAL PRIMARY KEY, name VARCHAR(200) NOT NULL, city VARCHAR(100), founded_year INT);');

INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql) VALUES
('METHODOLOGY', 1, 'Занятие 1. Создание таблиц', 2, '1.2. Добавление столбца isbn',
 'Добавьте в таблицу books столбец isbn VARCHAR(13).',
 5, 'ALTER TABLE ... ADD COLUMN', 'ALTER TABLE books ADD COLUMN isbn VARCHAR(13);');

-- Одна задача из ADVANCED (упрощённая, без многострочных кавычек)
INSERT INTO tasks (section, lesson_number, lesson_title, task_number, title, text, points, hint_text, correct_sql) VALUES
('ADVANCED', 1, 'Повышенный уровень', 1, 'Рекурсивный CTE для иерархии',
 'Создайте рекурсивный запрос для вывода категорий. Таблица categories: id SERIAL PRIMARY KEY, name VARCHAR(100), parent_id INT REFERENCES categories(id).',
 15, 'Используйте WITH RECURSIVE', 'WITH RECURSIVE cat_tree AS (SELECT id, name, parent_id, 1 AS level FROM categories WHERE parent_id IS NULL UNION ALL SELECT c.id, c.name, c.parent_id, ct.level+1 FROM categories c JOIN cat_tree ct ON c.parent_id = ct.id) SELECT * FROM cat_tree;');