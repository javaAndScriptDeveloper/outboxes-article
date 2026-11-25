-- Part 7: ordering value assigned by the database, independent of any instance's clock
ALTER TABLE outbox_tasks ADD COLUMN seq BIGSERIAL;

-- Part 8: partial indexes for the three background operations
CREATE INDEX idx_outbox_fetch
    ON outbox_tasks (seq ASC)
    WHERE state IN ('PENDING', 'FAILED');

CREATE INDEX idx_outbox_stuck
    ON outbox_tasks (heartbeat_at ASC, created_at ASC)
    WHERE state = 'PROCESSING';

CREATE INDEX idx_outbox_cleanup
    ON outbox_tasks (updated_at ASC)
    WHERE state = 'COMPLETED';
