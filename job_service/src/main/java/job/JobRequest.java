package job;

public record JobRequest(
    UUID UserId,
    String Schedule
) {}
