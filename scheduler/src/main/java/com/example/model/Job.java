package com.example.model;

import java.sql.Timestamp;
import java.util.UUID;

public record Job(
    UUID JobId, 
    String Schedule,
    int retryCount,
    int maxRetries,
    Timestamp createdAt,
    Timestamp nextRun
) {}
