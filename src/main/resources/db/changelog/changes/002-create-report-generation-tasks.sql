CREATE TABLE report_generation_tasks (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    state      task_state  NOT NULL    DEFAULT 'PENDING',
    payload    JSONB       NOT NULL,
    created_at TIMESTAMPTZ NOT NULL    DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL    DEFAULT now()
);
