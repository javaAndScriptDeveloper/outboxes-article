CREATE INDEX idx_rgt_fetch
    ON report_generation_tasks (created_at ASC)
    WHERE state IN ('PENDING', 'FAILED');

CREATE INDEX idx_rgt_stuck
    ON report_generation_tasks (heartbeat_at ASC, created_at ASC)
    WHERE state = 'PROCESSING';

CREATE INDEX idx_rgt_cleanup
    ON report_generation_tasks (updated_at ASC)
    WHERE state = 'COMPLETED';
