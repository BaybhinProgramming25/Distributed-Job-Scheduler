package job;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;

import static com.cronutils.model.CronType.UNIX;

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
                Timestamp nextTime = getNextRunTime(job.createdAt(), job.Schedule());
                NextJob nextJob = insertNextScheduledJob(job, nextTime);
                
                if (next_job == null) {
                    ctx.status(201).json(Map.of("job", job)); 
                } 
                ctx.status(201).json(Map.of(
                    "job", job,
                    "next_job", next_job
                ));
            });
        }).start(7000);

    }

    // Submits the job onto the database after getting a connection 
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
                System.out.println("Couldn't add to database!");
            }
        }
        catch (SQLException e) {
            throw new RuntimeException("Failed to insert job: ", e);
        }
        
        // Return the job
        return job;
    }

    // After calculating the next run time, add the new job 
    public static void insertNextScheduledJob(Job job, Timestamp nextRunTime) {

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
                System.out.println("Failed to insert into Database");
            }
        }
        catch (SQLException e) {
            throw new RuntimeException("Failed to insert next job: ", e);
        }
    }


    public static Timestamp getNextRunTime(Timestamp currentTime, String cronString) {

        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(UNIX));

        Cron cron = parser.parse(cronString); 

        ZonedDateTime nowUTC = currentTime.toInstant().atZone(ZoneOffset.UTC);

        ZonedDateTime nextUTC = ExecutionTime.forCron(cron).nextExecution(nowUTC).orElse(null);

        if (nextUTC == null) { 
            return null;
        }

        return Timestamp.from(nextUTC.toInstant());

    }
}
