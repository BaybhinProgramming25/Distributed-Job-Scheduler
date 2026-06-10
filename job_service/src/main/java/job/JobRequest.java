package job;

// Shape of the JSON payload the frontend sends when creating a job
public record JobRequest(
    String UserId,
    String Name,
    String Type,
    boolean isRecurring,
    String Schedule
) {}
