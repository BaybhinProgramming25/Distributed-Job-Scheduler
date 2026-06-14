package job;

import io.javalin.Javalin;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {

    private static final int DEFAULT_MAX_RETRIES = 10;

    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> rule.anyHost()));
        }).start(7000);

        // Create a new job
        app.post("/job", ctx -> {
            JobRequest request = ctx.bodyAsClass(JobRequest.class);
            Job job = insertJob(request);
            ctx.status(201).json(job);
        });
    }

    public static Job insertJob(JobRequest request) {
        Job job = new Job(
            request.UserId(),
            UUID.randomUUID(),
            request.Name(),
            request.Type(),
            "Scheduled",
            request.isRecurring(),
            request.Schedule(),
            0,
            DEFAULT_MAX_RETRIES,
            Timestamp.from(Instant.now())
        );

        // Create the job for now, add the database logic later on 
        System.out.println("Received job: " + job);
    }
}
