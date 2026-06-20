package job;

import java.sql.Timestamp;
import java.util.UUID;

public record NextJob(
    UUID nextJobId,
    Timestamp nextRunTime,
    UUID jobId
) {}