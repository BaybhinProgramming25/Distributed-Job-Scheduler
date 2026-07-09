package com.example.model;

import java.sql.Timestamp;
import java.util.UUID;

public record JobDTO(
    UUID id,
    String schedule,
    int retriesCount,
    int maxRetries,
    Timestamp nextRun,
    String jobActive
) {}
