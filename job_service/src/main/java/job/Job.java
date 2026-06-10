package job;

import java.sql.Timestamp;
import java.util.UUID;

public record Job(
    String UserId, // Received from frontend
    UUID Id, // Made when the job is made
    String Name, // Received from frontend
    String Type, // Received from frontend
    String Status, // Set to "Scheduled" when job is created
    boolean isRecurring, // Received from frontend
    String Schedule, // Received from frontend
    int retryCount,
    int maxRetries, // Should set a default amount
    Timestamp createdAt // Made when the job is made
) {}
