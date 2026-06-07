ALTER TYPE task_state ADD VALUE 'PROCESSING';
ALTER TYPE task_state ADD VALUE 'FAILED';

ALTER TABLE report_generation_tasks
    ADD COLUMN retry_count    INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN error_message  VARCHAR,
    ADD COLUMN version        BIGINT  NOT NULL DEFAULT 0;
