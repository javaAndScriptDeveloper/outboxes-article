CREATE TABLE generated_reports (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key UUID        NOT NULL,
    report_id       VARCHAR     NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL    DEFAULT now(),

    CONSTRAINT uq_generated_reports_idempotency_key UNIQUE (idempotency_key)
);
