package job;

import java.sql.Timestamp;
import java.util.UUID;

public record Job(
    UUID UserId, // Job that is associated with the user 
    UUID JobId, // Job ID 
    String Schedule, // CRON string
    int retryCount, // Keep track of the retries 
    int maxRetries, // Should set a default amount
    Timestamp createdAt // Time in UTC 
) {}
