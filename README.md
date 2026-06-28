# Transactional Outbox Demo

**Code companion for the article:** [**"The Transactional Outbox Pattern: From a Single Task to a Production-Grade Async Engine"**](https://codewithkyryl.dev/2026/06/07/outboxes/)

This repository contains working examples demonstrating the transactional outbox pattern at each stage of the article—from basic task persistence through a production-ready async engine with heartbeat recovery, exponential backoff, and multi-type dispatch.

**Quick links once running:**
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090
- Actuator metrics: http://localhost:8080/actuator/prometheus
---
## Prerequisites
- Docker
- Java 21
---
## Running
**Step 1 — Start the infrastructure:**
```bash
docker compose up -d
```
This starts:
- `postgres-app` on port **5434** (application database)
- `postgres-test` on port **5435** (test database)
- Prometheus on port **9090** (scrapes app metrics every 5s)
- Grafana on port **3000** (pre-configured dashboard, no login required)
**Step 2 — Start the application:**
```bash
./gradlew bootRun
```
---
## Submitting a report task
```bash
curl -X POST http://localhost:8080/reports \
  -H 'Content-Type: application/json' \
  -d '{"from":"2024-01-01T00:00:00Z","to":"2024-12-31T23:59:59Z"}'
```
The endpoint returns `202 Accepted`. The outbox worker picks up the task within 1 second and logs the result. Watch the **Queue Depth** and **Throughput** panels in Grafana update within a few seconds.
---
## Observability
Open Grafana at http://localhost:3000 — the **Transactional Outbox** dashboard loads automatically with four panels:
| Panel | What it shows |
|-------|--------------|
| Queue Depth | Live count of PENDING + FAILED tasks |
| Completed (last 5 min) | Tasks successfully processed |
| Failed (last 5 min) | Tasks that errored |
| Processing Latency p50/p95/p99 | End-to-end task duration |
---
## Tests
Tests run against the `postgres-test` container (port 5435). Make sure `docker compose up -d` has been run first.
```bash
./gradlew test
```
Each test class corresponds to an article section:
| Test class | Article part |
|-----------|-------------|
| `DemoApplicationTests` | Part 1 — basic entity persistence |
| `Part2SkipLockedConcurrencyTest` | Part 2 — SKIP LOCKED prevents double-claiming |
| `ReportGenerationTaskServiceProcessingStatusTest` | Part 2/3 — PROCESSING state and version increment |
| `Part3RetryAndFailureTest` | Part 3 — retry count gates eligibility |
| `Part4ExponentialBackoffTest` | Part 4 — execute_at controls retry timing |
| `Part5HeartbeatStuckTaskTest` | Part 5 — stale heartbeat triggers recovery |
| `Part6MultiTypeDispatchTest` | Part 6 — dispatcher routes to correct handler |
| `OutboxTaskIdempotencyTest` | Part 10 — idempotency prevents duplicate work |
