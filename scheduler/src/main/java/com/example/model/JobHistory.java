package com.example.model;

import java.sql.Timestamp;
import java.util.UUID;

public record JobHistory(
    UUID jobId,
    UUID histoyId,
    String schedule,
    int retriesCount,
    int maxRetries,
    Timestamp nextRun
) {}
