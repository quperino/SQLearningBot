package ru.quperino.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.quperino.entities.ApplicationUser;
import ru.quperino.entities.Task;
import ru.quperino.entities.UserTaskSolution;
import ru.quperino.entities.enums.SolutionStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для хранения попыток решений задач пользователями.
 * Содержит сложные запросы для отслеживания активных сессий, получения статистики,
 * управления тренировочным режимом и экспорта истории.
 */
public interface UserTaskSolutionRepository extends JpaRepository<UserTaskSolution, Long> {

    // ------------------------------------------------------------------------
    // 1. Методы для работы с активными (текущими) решениями
    // ------------------------------------------------------------------------

    /**
     * Находит самое свежее (по дате создания) решение пользователя с указанным статусом.
     * <p>
     * Применение:
     * <ul>
     *   <li>В {@code NodeMainServiceImpl.findActiveSolution()} – ищется PENDING или PROCESSING решение,
     *   чтобы определить, есть ли у пользователя незавершённая задача.</li>
     *   <li>В {@code CallbackQueryProcessorServiceImpl.handleHint()} – ищется активное решение,
     *   чтобы отметить использование подсказки.</li>
     * </ul>
     *
     * @param user   пользователь
     * @param status искомый статус (чаще всего PENDING или PROCESSING)
     * @return {@code Optional} с решением или пустой, если ничего не найдено
     */
    Optional<UserTaskSolution> findTopByUserAndStatusOrderByCreatedAtDesc(ApplicationUser user, SolutionStatus status);

    /**
     * Возвращает все решения пользователя с указанным статусом (без сортировки).
     * <p>
     * Используется в:
     * <ul>
     *   <li>{@code UserServiceImpl.getUserStatistics()} – собираются все COMPLETED решения для подсчёта баллов.</li>
     *   <li>{@code UserServiceImpl.clearUserSession()} – находятся все PENDING и PROCESSING решения,
     *   чтобы отменить их при сбросе сессии.</li>
     * </ul>
     *
     * @param user   пользователь
     * @param status статус решений
     * @return список решений (может быть пустым)
     */
    List<UserTaskSolution> findByUserAndStatus(ApplicationUser user, SolutionStatus status);

    /**
     * Находит решение по трём параметрам: ID пользователя, ID задачи и статус.
     * <p>
     * Ключевой метод для безопасности: после того как AI вернул ответ,
     * проверяется, что решение всё ещё находится в статусе PROCESSING
     * (не было отменено за время проверки). Если статус изменился – ответ игнорируется.
     *
     * @param userId ID пользователя
     * @param taskId ID задачи
     * @param status ожидаемый статус (обычно PROCESSING)
     * @return {@code Optional} с решением или пустой
     */
    Optional<UserTaskSolution> findByUser_IdAndTask_IdAndStatus(Long userId, Long taskId, SolutionStatus status);

    /**
     * Возвращает все решения пользователя (любые статусы).
     * <p>
     * Используется только в {@code UserServiceImpl.resetUserData()} для удаления всех решений
     * при полном сбросе данных.
     *
     * @param user пользователь
     * @return список всех решений пользователя
     */
    List<UserTaskSolution> findByUser(ApplicationUser user);

    // ------------------------------------------------------------------------
    // 2. Методы для проверки факта завершения задачи
    // ------------------------------------------------------------------------

    /**
     * Проверяет, существует ли хотя бы одно решение пользователя по данной задаче
     * с указанным статусом (чаще всего COMPLETED).
     * <p>
     * Используется:
     * <ul>
     *   <li>В {@code CallbackQueryProcessorServiceImpl.processCallback()} – при выборе задачи
     *   проверяется, решена ли она уже, чтобы переключиться в тренировочный режим.</li>
     *   <li>В {@code NodeMainServiceImpl.handleTrainingSolutionSubmission()} – убеждается,
     *   что задача действительно была решена ранее, прежде чем разрешить тренировку.</li>
     * </ul>
     *
     * @param user   пользователь
     * @param task   задача
     * @param status искомый статус
     * @return {@code true}, если есть хотя бы одна запись с таким статусом
     */
    boolean existsByUserAndTaskAndStatus(ApplicationUser user, Task task, SolutionStatus status);

    /**
     * Находит самое свежее (по дате создания) решение пользователя по конкретной задаче
     * с указанным статусом.
     * <p>
     * Применяется в {@code UserServiceImpl.resetTaskProgress()} – чтобы найти завершённое
     * (COMPLETED) решение для последующего сброса.
     *
     * @param user   пользователь
     * @param task   задача
     * @param status статус (обычно COMPLETED)
     * @return {@code Optional} с решением или пустой
     */
    Optional<UserTaskSolution> findTopByUserAndTaskAndStatusOrderByCreatedAtDesc(ApplicationUser user, Task task, SolutionStatus status);

    // ------------------------------------------------------------------------
    // 3. Методы для статистики по секциям и занятиям
    // ------------------------------------------------------------------------

    /**
     * Подсчитывает количество решений пользователя в указанной секции с заданным статусом.
     * <p>
     * Используется в {@code UserServiceImpl.getUserStatistics()} для быстрого получения
     * количества решённых задач в секции "ADVANCED".
     *
     * @param user    пользователь
     * @param section название секции ("METHODOLOGY" или "ADVANCED")
     * @param status  статус (обычно COMPLETED)
     * @return количество решений
     */
    @Query("SELECT COUNT(s) FROM UserTaskSolution s JOIN s.task t WHERE s.user = :user AND t.section = :section AND s.status = :status")
    long countByUserAndTaskSectionAndStatus(@Param("user") ApplicationUser user, @Param("section") String section, @Param("status") SolutionStatus status);

    /**
     * Подсчитывает количество решений пользователя в секции с любым из переданных статусов.
     * <p>
     * В текущей версии не используется, но может пригодиться для сложных отчётов.
     *
     * @param user     пользователь
     * @param section  секция
     * @param statuses коллекция возможных статусов
     * @return количество решений
     */
    @Query("SELECT COUNT(s) FROM UserTaskSolution s JOIN s.task t WHERE s.user = :user AND t.section = :section AND s.status IN (:statuses)")
    long countByUserAndTaskSectionAndStatusIn(@Param("user") ApplicationUser user, @Param("section") String section, @Param("statuses") Collection<SolutionStatus> statuses);

    // ------------------------------------------------------------------------
    // 4. Методы для истории и пагинации
    // ------------------------------------------------------------------------

    /**
     * Находит все решения пользователя по конкретной задаче (любой статус).
     * <p>
     * Используется в {@code CallbackQueryProcessorServiceImpl.processCallback()} – перед созданием
     * нового решения удаляются все старые записи по этой задаче, чтобы избежать конфликтов.
     *
     * @param user пользователь
     * @param task задача
     * @return список решений (может быть пустым)
     */
    List<UserTaskSolution> findByUserAndTask(ApplicationUser user, Task task);

    /**
     * Находит самое свежее (по дате завершения) решение пользователя по задаче с указанным статусом.
     * <p>
     * Отличается от {@code findTopByUserAndTaskAndStatusOrderByCreatedAtDesc} тем, что сортирует
     * по полю {@code completedAt} (момент получения результата от AI), а не по {@code createdAt}.
     * <p>
     * Применяется в тренировочном режиме для получения последнего правильного решения,
     * чтобы показать его пользователю.
     *
     * @param user   пользователь
     * @param task   задача
     * @param status статус (обычно COMPLETED)
     * @return {@code Optional} с решением или пустой
     */
    Optional<UserTaskSolution> findFirstByUserAndTaskAndStatusOrderByCompletedAtDesc(ApplicationUser user, Task task, SolutionStatus status);

    /**
     * Возвращает страницу решений пользователя, отсортированных по дате завершения (от новых к старым).
     * <p>
     * Используется в {@code UserServiceImpl.getUserHistory()} для реализации команды {@code /history}
     * с пагинацией (по 5 записей на страницу).
     *
     * @param user     пользователь
     * @param pageable объект пагинации (номер страницы, размер)
     * @return страница с объектами {@code UserTaskSolution}
     */
    Page<UserTaskSolution> findByUserOrderByCompletedAtDesc(ApplicationUser user, Pageable pageable);

    /**
     * Возвращает все решения пользователя, отсортированные по дате завершения (от новых к старым).
     * <p>
     * Используется в {@code ExportServiceImpl.generateExportCsv()} – для выгрузки всей истории
     * решений без пагинации в CSV-файл.
     *
     * @param user пользователь
     * @return список всех решений
     */
    List<UserTaskSolution> findAllByUserOrderByCompletedAtDesc(ApplicationUser user);
}
