ALTER TABLE report_generation_tasks
    ADD COLUMN execute_at TIMESTAMPTZ NOT NULL DEFAULT now();
