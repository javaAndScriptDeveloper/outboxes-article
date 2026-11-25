package com.example.demo.singletype;

import com.example.demo.Jitter;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.example.demo.TaskState;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private static final Duration BULK_JITTER_WINDOW = Duration.ofMinutes(10);

    private final ReportGenerationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void submit(ReportGenerationCommand command) {
        var entity = repository.save(ReportGenerationEntity.builder()
                .state(TaskState.PENDING)
                .payload(ReportGenerationPayload.builder()
                        .from(command.getFrom())
                        .to(command.getTo())
                        .build())
                .retryCount(0)
                .executeAt(Instant.now())
                .build());

        eventPublisher.publishEvent(new ReportGenerationTaskSubmittedEvent(entity.getId()));
    }

    /** Zero-select (Part 2.5): insert already PROCESSING with a lease, ship the payload in the event. */
    @Transactional
    public void submitZeroSelect(ReportGenerationCommand command) {
        var payload = ReportGenerationPayload.builder()
                .from(command.getFrom())
                .to(command.getTo())
                .build();
        var leasedAt = Instant.now().truncatedTo(ChronoUnit.MICROS); // Postgres stores microseconds
        var entity = repository.save(ReportGenerationEntity.builder()
                .state(TaskState.PROCESSING)
                .heartbeatAt(leasedAt)        // lease: the poller leaves this row alone
                .payload(payload)
                .retryCount(0)
                .executeAt(Instant.now())
                .build());
        eventPublisher.publishEvent(new ReportRequestedEvent(entity.getId(), leasedAt, payload));
    }

    /** Bulk / non-urgent submission (Part 4): spread over the next 10 minutes instead of "all now". */
    @Transactional
    public void submitBulk(List<ReportGenerationCommand> commands) {
        var now = Instant.now();
        repository.saveAll(commands.stream()
                .map(command -> ReportGenerationEntity.builder()
                        .state(TaskState.PENDING)
                        .payload(ReportGenerationPayload.builder()
                                .from(command.getFrom())
                                .to(command.getTo())
                                .build())
                        .retryCount(0)
                        .executeAt(Jitter.spreadOver(BULK_JITTER_WINDOW, now))
                        .build())
                .toList());
    }
}
