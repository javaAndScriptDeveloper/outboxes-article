package com.example.demo.multitype;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "generated_reports")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeneratedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    UUID idempotencyKey;

    @Column(nullable = false, updatable = false)
    String reportId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    Instant createdAt;
}
