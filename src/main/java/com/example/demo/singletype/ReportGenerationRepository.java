package com.example.demo.singletype;

import com.example.demo.SkipLocked;
import com.example.demo.TaskState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportGenerationRepository extends JpaRepository<ReportGenerationEntity, UUID> {

    @SkipLocked
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM ReportGenerationEntity t " +
           "WHERE t.state IN :states " +
           "AND t.retryCount < :maxRetryCount " +
           "AND t.executeAt <= :now " +
           "ORDER BY t.createdAt ASC, t.id ASC " +
           "LIMIT 1")
    Optional<ReportGenerationEntity> findFirstEligibleTask(
            @Param("states") List<TaskState> states,
            @Param("maxRetryCount") Integer maxRetryCount,
            @Param("now") Instant now);

    @SkipLocked
    @Lock(LockModeType.PESSIMISTIC_WRITE) // two recovery runs never reset the same task
    Optional<ReportGenerationEntity> findFirstByStateAndHeartbeatAtLessThanOrderByCreatedAt(
            TaskState state, Instant heartbeatAt);

    @Modifying
    @Transactional
    @Query("UPDATE ReportGenerationEntity t SET t.heartbeatAt = :now WHERE t.id = :id")
    void updateHeartbeatAt(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("DELETE FROM ReportGenerationEntity t WHERE t.state IN :states AND t.updatedAt < :before")
    int deleteByStateInAndUpdatedAtBefore(@Param("states") List<TaskState> states, @Param("before") Instant before);

    long countByStateInAndRetryCountLessThan(List<TaskState> states, int maxRetryCount);

    long countByStateAndRetryCountGreaterThanEqual(TaskState state, int maxRetryCount);

    /** Zero-select: take over the lease only if nobody (e.g. stuck-task recovery) has touched it since insert. */
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            UPDATE report_generation_tasks
               SET heartbeat_at = now(), updated_at = now()
             WHERE id = :id AND state = 'PROCESSING' AND heartbeat_at = :leasedAt
            """)
    int renewLease(@Param("id") UUID id, @Param("leasedAt") Instant leasedAt);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            UPDATE report_generation_tasks SET state = 'COMPLETED', updated_at = now()
             WHERE id = :id AND state = 'PROCESSING'
            """)
    int markCompleted(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            UPDATE report_generation_tasks SET state = 'COMPLETED', updated_at = now()
             WHERE id IN (:ids) AND state = 'PROCESSING'
            """)
    int markCompleted(@Param("ids") List<UUID> ids);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = """
            UPDATE report_generation_tasks
               SET state = 'FAILED', retry_count = retry_count + 1, error_message = :error,
                   execute_at = now() + interval '1 minute', updated_at = now()
             WHERE id = :id AND state = 'PROCESSING'
            """)
    int markFailed(@Param("id") UUID id, @Param("error") String error);
}
