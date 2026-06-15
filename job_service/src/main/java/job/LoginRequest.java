package job;

public record LoginRequest(
    String email,
    String hashed_password
) {}