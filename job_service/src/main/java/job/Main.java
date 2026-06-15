package job;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import database.Database; 

public class Main {

    private static final int STARTING_MAX_RETRIES = 0;
    private static final int MAX_RETRIES_LIMIT = 10;

    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {

            config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> rule.anyHost()));

            config.routes.post("/job", ctx -> {
                JobRequest request = ctx.bodyAsClass(JobRequest.class);
                Job job = jobSubmission(request);
                ctx.status(201).json(job);
            });
        }).start(7000);

    }

    // Function is void for now 
    public static Job jobSubmission(JobRequest request) {

        // Create the job 
        Job job = new Job(UUID.randomUUID(), request.Schedule(), STARTING_MAX_RETRIES, MAX_RETRIES_LIMIT, Timestamp.from(Instant.now()));

        // Setup database connection



        // Connect to Cockroach DB somehow then insert the job and verify if successfully inserted 


        // Create the job for now, add the database logic later on 
        System.out.println("Received job: " + job);

        // Return the job
        return job;
    }

    public static void insertNextScheduledJob(Job job) {

        // Insert the next scheduled job after we have inserted the current job 

        System.out.println("Pass");
    }


    public static Timestamp getNextTime(String cronSchedule) {
        
        // Placeholder return value 
        return Timestamp.from(Instant.now());
    }
}
