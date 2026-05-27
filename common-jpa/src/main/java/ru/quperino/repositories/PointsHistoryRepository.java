package ru.quperino.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.quperino.entities.PointsHistory;

/**
 * Репозиторий для истории начисления и списания баллов.
 * Позволяет в будущем реализовать детализированные отчёты по активности пользователя.
 */
public interface PointsHistoryRepository extends JpaRepository<PointsHistory, Long> {
    // В текущей версии проекта дополнительные методы не требуются.
}
