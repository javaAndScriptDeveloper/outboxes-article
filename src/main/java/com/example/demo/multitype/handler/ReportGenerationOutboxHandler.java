package com.example.demo.multitype.handler;

import com.example.demo.multitype.GeneratedReport;
import com.example.demo.multitype.GeneratedReportRepository;
import com.example.demo.multitype.OutboxTaskHandler;
import com.example.demo.multitype.OutboxTaskType;
import com.example.demo.singletype.ReportGenerationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportGenerationOutboxHandler implements OutboxTaskHandler<ReportGenerationPayload> {

    private final GeneratedReportRepository generatedReportRepository;

    @Override
    public OutboxTaskType getType() {
        return OutboxTaskType.REPORT_GENERATION;
    }

    @Override
    public Class<ReportGenerationPayload> payloadClass() {
        return ReportGenerationPayload.class;
    }

    @Override
    public void handle(UUID taskId, ReportGenerationPayload payload) {
        if (generatedReportRepository.findByIdempotencyKey(taskId).isPresent()) {
            log.info("Report already generated for outbox task {} — skipping (idempotent)", taskId);
            return;
        }

        var reportId = "report-" + taskId;
        log.info("Generating report {} from {} to {}", reportId, payload.getFrom(), payload.getTo());

        generatedReportRepository.save(GeneratedReport.builder()
                .idempotencyKey(taskId)
                .reportId(reportId)
                .build());
    }
}
