package ru.quperino.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Сущность, представляющая задачу (упражнение) по SQL.
 * Задачи привязаны к секции (Методичка / Повышенный уровень), занятию и номеру задачи.
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Секция: "METHODOLOGY" или "ADVANCED"
    private String section;

    // Название задачи (например, "1.1. Создание таблицы publishers")
    @Column(length = 500)
    private String title;

    // Текст условия задачи
    @Column(columnDefinition = "TEXT")
    private String text;

    // Количество баллов за верное решение
    private int points;

    // Текст подсказки
    @Column(columnDefinition = "TEXT")
    private String hintText;

    // Эталонный SQL-запрос (правильное решение)
    @Column(columnDefinition = "TEXT")
    private String correctSql;

    // Номер занятия в рамках секции (только для METHODOLOGY)
    private Integer lessonNumber;

    // Название занятия (например, "Занятие 1. Создание и удаление таблиц")
    @Column(length = 500)
    private String lessonTitle;

    // Номер задачи внутри занятия (1, 2, ...)
    private Integer taskNumber;
}
