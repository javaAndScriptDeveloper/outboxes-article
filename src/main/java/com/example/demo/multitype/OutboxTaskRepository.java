package com.example.demo.multitype;

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

public interface OutboxTaskRepository extends JpaRepository<OutboxTaskEntity, UUID> {

    @SkipLocked
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM OutboxTaskEntity t " +
           "WHERE t.state IN :states " +
           "AND t.retryCount <= :maxRetryCount " +
           "AND t.executeAt <= :now " +
           "ORDER BY t.createdAt ASC, t.id ASC LIMIT 1")
    Optional<OutboxTaskEntity> findFirstEligibleTask(
            @Param("states") List<TaskState> states,
            @Param("maxRetryCount") Integer maxRetryCount,
            @Param("now") Instant now);

    Optional<OutboxTaskEntity> findFirstByStateAndHeartbeatAtLessThanOrderByCreatedAt(TaskState state, Instant heartbeatAt);

    @Modifying
    @Transactional
    @Query("UPDATE OutboxTaskEntity t SET t.heartbeatAt = :now WHERE t.id = :id")
    void updateHeartbeatAt(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("DELETE FROM OutboxTaskEntity t WHERE t.state IN :states AND t.updatedAt < :before")
    int deleteByStateInAndUpdatedAtBefore(@Param("states") List<TaskState> states, @Param("before") Instant before);
}
