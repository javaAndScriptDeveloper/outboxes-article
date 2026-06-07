package com.example.demo.multitype;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, UUID> {
    Optional<GeneratedReport> findByIdempotencyKey(UUID idempotencyKey);
}
