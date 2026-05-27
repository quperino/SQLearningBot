-- =========================================================
-- Создание служебных таблиц бота и вспомогательных объектов
-- =========================================================

-- 1. Таблица пользователей
CREATE TABLE IF NOT EXISTS application_user (
    id BIGSERIAL PRIMARY KEY,
    telegram_user_id VARCHAR(255) NOT NULL,
    first_login_date TIMESTAMP,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    username VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    is_active BOOLEAN,
    user_state VARCHAR(50),
    total_points INT DEFAULT 0,
    training_task_id BIGINT
);

-- 2. Таблица задач
CREATE TABLE IF NOT EXISTS tasks (
    id BIGSERIAL PRIMARY KEY,
    section VARCHAR(255),
    title VARCHAR(500),
    text TEXT,
    points INT,
    hint_text TEXT,
    correct_sql TEXT,
    lesson_number INT,
    lesson_title VARCHAR(500),
    task_number INT,
    CONSTRAINT unique_task_per_lesson UNIQUE (section, lesson_number, task_number)
);

-- 3. Решения пользователей
CREATE TABLE IF NOT EXISTS user_task_solutions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES application_user(id) ON DELETE CASCADE,
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    status VARCHAR(50),
    ai_feedback TEXT,
    created_at TIMESTAMP,
    completed_at TIMESTAMP,
    last_correct_solution TEXT,
    attempts INT DEFAULT 0,
    hint_used BOOLEAN DEFAULT FALSE,
    task_message_id INT,
    CONSTRAINT unique_user_task UNIQUE (user_id, task_id)
);

-- 4. Сообщения пользователя
CREATE TABLE IF NOT EXISTS user_messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES application_user(id) ON DELETE CASCADE,
    message_text TEXT,
    created_at TIMESTAMP,
    processing_time_ms BIGINT
);

-- 5. Сообщения бота
CREATE TABLE IF NOT EXISTS bot_messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES application_user(id) ON DELETE CASCADE,
    message_text TEXT,
    created_at TIMESTAMP,
    processing_time_ms BIGINT
);

-- 6. Клики по кнопкам (для аналитики)
CREATE TABLE IF NOT EXISTS button_clicks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES application_user(id) ON DELETE CASCADE,
    task_id BIGINT REFERENCES tasks(id) ON DELETE SET NULL,
    button_name VARCHAR(255),
    created_at TIMESTAMP
);

-- 7. История начисления/списания баллов
CREATE TABLE IF NOT EXISTS points_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES application_user(id) ON DELETE CASCADE,
    task_id BIGINT REFERENCES tasks(id) ON DELETE SET NULL,
    points INT,
    created_at TIMESTAMP
);

-- Индексы для ускорения запросов (по внешним ключам и часто используемым полям)
CREATE INDEX IF NOT EXISTS idx_user_task_solutions_user_id ON user_task_solutions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_task_solutions_task_id ON user_task_solutions(task_id);
CREATE INDEX IF NOT EXISTS idx_user_messages_user_id ON user_messages(user_id);
CREATE INDEX IF NOT EXISTS idx_bot_messages_user_id ON bot_messages(user_id);
CREATE INDEX IF NOT EXISTS idx_button_clicks_user_id ON button_clicks(user_id);
CREATE INDEX IF NOT EXISTS idx_points_history_user_id ON points_history(user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_section ON tasks(section);
CREATE INDEX IF NOT EXISTS idx_tasks_lesson_number ON tasks(lesson_number);