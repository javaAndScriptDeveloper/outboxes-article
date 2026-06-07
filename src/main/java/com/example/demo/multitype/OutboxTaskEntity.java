package com.example.demo.multitype;

import com.example.demo.TaskState;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "outbox_tasks")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutboxTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    OutboxTaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TaskState state;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    String payload;

    String errorMessage;

    Integer retryCount;

    @Version
    Long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    Instant updatedAt;

    Instant heartbeatAt;

    Instant executeAt;
}
