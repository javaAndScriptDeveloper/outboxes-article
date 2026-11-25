package com.example.demo;

import com.example.demo.singletype.ReportGenerationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class Part12PayloadVersioningTest extends BaseIntegrationTest {

    @Autowired
    private ReportGenerationRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM report_generation_tasks"); // a poison row can't be loaded by deleteAll()
    }

    @Test
    void rowWrittenByANewerVersionWithAnExtraFieldStillLoads() {
        var id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO report_generation_tasks (id, state, payload)
                VALUES (?, 'PENDING', '{"from":"2024-01-01T00:00:00Z","to":"2024-01-31T23:59:59Z","format":"pdf"}')
                """, id);

        var loaded = repository.findById(id).orElseThrow();

        assertThat(loaded.getPayload().getFrom()).hasToString("2024-01-01T00:00:00Z");
    }
}
