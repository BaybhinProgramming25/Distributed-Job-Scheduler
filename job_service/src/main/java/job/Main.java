package job;

import io.javalin.Javalin;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Main {

    private static final int STARTING_MAX_RETRIES = 0;
    private static final int MAX_RETRIES_LIMIT = 10;

    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> rule.anyHost()));
        }).start(7000);

        app.post("/login", ctx -> {
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);
            ctx.status(201).result("Logged In Successfully!");
        });

        app.post("/signup", ctx -> {
            SignUpRequest request = ctx.bodyAsClass(SignUpRequest.class);
            ctx.status(201).result("User Created Successfully!");
        });

        app.post("/job", ctx -> {
            JobRequest request = ctx.bodyAsClass(JobRequest.class);
            Job job = insertJob(request);
            ctx.status(201).json(job);
        });
    }

    public static void checkLogin() {
        System.out.println("Pass");
    }

    public static void insertUser() {
        // Need to implement the ogi
    }

    public static Job insertJob(JobRequest request) {
        Job job = new Job(
            request.UserId(), 
            UUID.randomUUID(), 
            request.Schedule(),
            STARTING_MAX_RETRIES, 
            MAX_RETRIES_LIMIT, 
            Timestamp.from(Instant.now()) 
        );

        // Add it to the CockroachDB at a later time 

        // Create the job for now, add the database logic later on 
        System.out.println("Received job: " + job);
    }
}
