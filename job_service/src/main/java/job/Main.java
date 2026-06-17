package job;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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

    public static Job jobSubmission(JobRequest request) {

        // Create the job 
        Job job = new Job(UUID.randomUUID(), request.Schedule(), STARTING_MAX_RETRIES, MAX_RETRIES_LIMIT, Timestamp.from(Instant.now()));

        // String SQL
        String sql = "INSERT INTO jobs (id, schedule, retries_count, max_retries, created_at) VALUES (?, ?, ?, ?, ?)";

        // Setup database connection
        try(
            Connection conn = Database.getDataSource().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setObject(1, job.JobId());
            stmt.setObject(2, job.Schedule());
            stmt.setObject(3, job.retryCount());
            stmt.setObject(4, job.maxRetries());
            stmt.setObject(5, job.createdAt());
            
            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted < 1) {
                System.out.Println("Couldn't add to database!");
            }
        }
        catch (SQLException e) {
            throw new runtimeException("Failed to insert job: ", e);
        }
        
        // Calculate the next time and insert it into the database 
        Timestamp nextTime = getNextRunTime(job.createdAt());

        // Insert the next job into the table 

        // Return the job
        return job;
    }

    public static Timestamp getNextRunTime(Timestamp currentTime) {


        // Add the cron-utils dependency later on 

    
        // Placeholder 
        return Timestamp.from(Instant.now());

    }

    public static void insertNextScheduledJob(Job job, Timestmap nextRunTime) {

        NextJob nextJob = new NextJob(UUID.randomUUID(), nextRunTime, job.JobId()); 

        String sql = "INSERT INTO next_jobs (id, next_job_time, job_id) VALUES (?, ?, ?)";

        try (
            Connection conn = Database.getDataSource().getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setObject(1, nextJob.nextJobId());
            stmt.setObject(2, nextJob.nextRunTime());
            stmt.setObject(3, nextJob.jobId());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated < 1) {
                System.out.Println("Failed to insert into Database");
            }
        }
    }
}
