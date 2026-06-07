package com.example.demo;

import com.example.demo.singletype.ReportGenerationEntity;
import com.example.demo.singletype.ReportGenerationPayload;
import com.example.demo.singletype.ReportGenerationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DemoApplicationTests extends BaseIntegrationTest {

    @Autowired
    ReportGenerationRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void entityIsPersistedAndRetrievedCorrectly() {
        var payload = ReportGenerationPayload.builder()
                .from(Instant.parse("2024-01-01T00:00:00Z"))
                .to(Instant.parse("2024-01-31T23:59:59Z"))
                .build();
        var entity = ReportGenerationEntity.builder()
                .state(TaskState.PENDING)
                .payload(payload)
                .retryCount(1)
                .executeAt(Instant.now())
                .build();

        var saved = repository.save(entity);
        repository.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        var found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getState()).isEqualTo(TaskState.PENDING);
        assertThat(found.getPayload().getFrom()).isEqualTo(payload.getFrom());
        assertThat(found.getPayload().getTo()).isEqualTo(payload.getTo());
    }
}
