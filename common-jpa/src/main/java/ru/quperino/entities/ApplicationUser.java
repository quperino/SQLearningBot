package ru.quperino.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.proxy.HibernateProxy;
import ru.quperino.entities.enums.UserStateEnum;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Сущность, представляющая пользователя бота. Хранит всю информацию о пользователе: Telegram-данные, email, состояние, баллы и т.д.
 */
@Entity
@Table(name = "application_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String telegramUserId;

    // Дата первого входа в бот (автоматически проставляется Hibernate)
    @CreationTimestamp
    private LocalDateTime firstLoginDate;
    private String firstName;
    private String lastName;
    private String username;

    // Email пользователя, уникальный
    @Column(unique = true)
    private String email;
    private boolean isActive;

    // Текущее состояние пользователя (ожидание email, решение задачи и т.п.)
    @Enumerated(EnumType.STRING)
    private UserStateEnum userState;

    // Общее количество набранных баллов
    @Builder.Default
    private Integer totalPoints = 0;

    // ID задачи, которая сейчас в тренировочном режиме (если есть)
    private Long trainingTaskId;

    // Методы equals и hashCode переопределены, чтобы Hibernate мог корректно сравнивать объекты даже тогда,
    // когда один из них обёрнут в прокси для ленивой загрузки. Сравнение идёт только по первичному ключу (id),
    // так как он уникален и стабилен для сохранённых записей.
    // Для новых, ещё не сохранённых объектов, id равен null, и они считаются разными.
    // Хеш-код строится на основе реального класса сущности (извлечённого из прокси) и id.
    // Это стандартная практика для JPA-сущностей с ленивой загрузкой.

    @Override
    public final boolean equals(Object o) {
        // 1. Сравнение по ссылке – если один и тот же объект, то равны
        if (this == o) {
            return true;
        }
        // 2. Если передан null – не равны
        if (o == null) {
            return false;
        }
        // 3. Определяем реальные классы сравниваемых объектов.
        // Hibernate может подставить вместо реального объекта свой прокси-класс.
        // Чтобы сравнение работало корректно, мы через HibernateProxy
        // извлекаем настоящий (persistent) класс сущности.
        Class<?> objectEffectiveClass = o instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        // Если реальные классы различны – объекты заведомо не равны
        if (thisEffectiveClass != objectEffectiveClass) {
            return false;
        }
        // 4. Приведение к типу сущности
        ApplicationUser that = (ApplicationUser) o;
        // 5. Сравниваем только по первичному ключу (id).
        // id уникален и неизменен для сохранённых записей.
        // Для новых (не сохранённых) объектов id = null, и они считаются разными,
        // что соответствует поведению JPA до сохранения в БД.
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        // Хеш-код вычисляется только по классу сущности (реальному, не прокси)
        // и по id. Это гарантирует, что для одного и того же объекта (даже если он
        // обёрнут в прокси) хеш-код будет одинаковым.
        // Если объект ещё не сохранён (id == null), хеш-код вычисляется от класса –
        // это приемлемо, так как у каждого transient-объекта будет свой уникальный
        // хеш (но в коллекциях такие объекты могут вести себя нестабильно, что
        // соответствует спецификации JPA).
        Class<?> effectiveClass = this instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        return Objects.hash(effectiveClass, getId());
    }
}
