# Distributed Job Runner

A distributed, asynchronous job scheduling system. A `scheduler` service polls for due jobs and publishes them to a message queue; a horizontally scalable pool of `worker` services consumes and executes them; a `job_service` REST API handles job creation and exposes live status; a React dashboard shows jobs, retries, and history in real time.

Scheduling and execution are fully decoupled — the scheduler only decides *when* a job is due and hands it off via RabbitMQ; workers only care about executing whatever message they receive. This lets worker capacity scale independently of the scheduling logic (see [Metrics](#metrics)).

## Architecture

```
              POST /job                    GET /job, /job/stream, /job/{id}/history
                 │                                        ▲
                 ▼                                        │ (SSE push, 2s interval)
          ┌─────────────┐                          ┌─────────────┐
          │ job_service │◄────────reads────────────│ job_service │
          └──────┬──────┘                          └──────┬──────┘
                 │ writes                                 │
                 ▼                                         │
          ┌─────────────┐        polls every 30s     ┌─────────────┐
          │ CockroachDB │◄────────────────────────────│  scheduler  │
          └──────┬──────┘                             └──────┬──────┘
                 ▲                                            │ publish
                 │ writes status/history                      ▼
          ┌─────────────┐        competing consumers   ┌─────────────┐
          │   worker    │◄──────────────────────────────│  RabbitMQ   │
          │ (replicable)│                                └─────────────┘
          └─────────────┘
```

Reliability notes:
- **Idempotent execution**: a `processedJobs` table (keyed on `jobId + executionTime`) guards against RabbitMQ's at-least-once redelivery, so a redelivered message can't re-execute a job that already ran.
- **Retries**: failed jobs increment `retriesCount` and are picked up again by the scheduler; jobs exceeding `maxRetries` are marked `dead` and stop being scheduled.
- **Live status** is derived (not stored) by joining each job against its most recent history row (`LEFT JOIN LATERAL`), so there's no separate state machine to keep in sync.

## Tech stack

| Layer | Technology |
|---|---|
| Services | Java 21, Spring Boot 4.0.7 (`job_service`, `scheduler`, `worker`) |
| Messaging | RabbitMQ 3 (competing consumers via Spring AMQP) |
| Database | CockroachDB (Postgres wire-compatible), accessed via Spring JDBC |
| Scheduling | `cron-utils` 9.2.1 (standard UNIX 5-field cron) |
| Live updates | Server-Sent Events (`SseEmitter`), no WebSocket/polling needed |
| Frontend | React 19, Vite 8 |
| Orchestration | Docker Compose (services individually scalable via `--scale`) |

## Running the project

**Prerequisites**: Docker, Node.js (for the client).

1. Start the backend stack:
   ```bash
   docker compose up -d --build
   ```
   This builds and starts `cockroachdb`, `rabbitmq`, `job-service`, `scheduler`, and `worker` (5 replicas by default, see `docker-compose.yml`).

2. Apply the schema — it isn't auto-mounted into the CockroachDB container, so run it once against a fresh volume:
   ```bash
   docker compose exec -T cockroachdb ./cockroach sql --insecure < schema/schema.sql
   ```

3. Start the frontend dev server:
   ```bash
   cd client
   npm install
   npm run dev
   ```
   Open **http://localhost:5173** — the dashboard tab shows live jobs; the Quick Send tab creates a test job.


### Scaling workers

Workers are stateless and horizontally scalable — no code or config changes needed:
```bash
docker compose up -d --scale worker={# of workers}
```

### Other useful endpoints

| Endpoint | Purpose |
|---|---|
| `GET /job` | List all jobs with derived status |
| `GET /job/{id}` | Single job detail |
| `GET /job/{id}/history` | Full execution history for a job |
| `GET /job/stream` | SSE stream of live job snapshots |
| RabbitMQ management UI | `http://localhost:15672` (guest/guest) |
| CockroachDB console | `http://localhost:8081` |

## Load testing

`loadtest/run.js` is a self-contained Node script that creates a batch of jobs, then tracks them through completion, reporting ingestion latency, end-to-end completion latency, throughput, and retry/failure rates.

```bash
cd loadtest
node run.js --jobs=200 --concurrency=20 --label=my-run
```

| Flag | Default | Meaning |
|---|---|---|
| `--jobs` | 100 | Number of jobs to create |
| `--concurrency` | 10 | Concurrent job-creation requests |
| `--leadSeconds` | 90 | How far in the future to schedule the batch (must exceed total ingestion time) |
| `--pollMs` | 2000 | Polling interval while waiting for completion |
| `--timeoutMs` | 900000 | Max time to wait before giving up on stragglers |

## Metrics

Measured locally (2 vCPU / 8GB dev container) by scaling `worker` replicas and running identical job batches through `loadtest/run.js`:

| Workers | Jobs | Completion throughput | P95 completion latency | Ingestion P95 | Failures |
|---|---|---|---|---|---|
| 1 | 15 | 2.21 jobs/min | 405s | 130ms | 0/15 |
| 5 | 15 | 5.12 jobs/min | 175s | 170ms | 0/15 |
| 8 | 1,000 | **13.84 jobs/min** | 4,105s* | 330ms | 197/1,000 (22%)** |

**\*** Not directly comparable to the smaller runs' P95 — with 1,000 jobs queued simultaneously, jobs near the back of the queue wait through genuine queueing delay, not processing slowness. **Throughput is the metric that's actually comparable across runs.**

**\*\*** Matches the worker's intentional synthetic fault injection (a 1%-per-second failure chance over a 10-40s simulated job, `1 - 0.99^25 ≈ 22%`) almost exactly — the pipeline behaving as designed under load, not an infrastructure issue.

**Result**: scaling from 1 → 8 workers produced a **~6.3x increase in job-completion throughput**, with zero container restarts or failed requests across all runs.
