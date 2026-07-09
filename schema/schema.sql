CREATE DATABASE IF NOT EXISTS dist_jobs_scheduler;

CREATE TABLE IF NOT EXISTS dist_jobs_scheduler.jobs(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule STRING NOT NULL, 
    retriesCount INTEGER NOT NULL DEFAULT 0,
    maxRetries INTEGER NOT NULL DEFAULT 10,
    nextRun TIMESTAMPTZ NOT NULL,
    jobActive TEXT NOT NULL DEFAULT 'active'
);

CREATE TABLE IF NOT EXISTS dist_jobs_scheduler.history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    jobId UUID NOT NULL REFERENCES dist_jobs_scheduler.jobs(id) ON DELETE CASCADE,
    jobStatus TEXT NOT NULL DEFAULT 'pending',
    jobStarted TIMESTAMPTZ NOT NULL,
    jobFinished TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS dist_jobs_scheduler.processedJobs (
    idempotency_key STRING PRIMARY KEY
);