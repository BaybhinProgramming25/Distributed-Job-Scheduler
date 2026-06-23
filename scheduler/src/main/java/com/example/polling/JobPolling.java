package com.example.polling;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.boot.CommandLineRunner;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.RowMapper; 

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.model.Job;

@Component
public class JobPolling implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate; 
    private final RabbitTemplate rabbitTemplate;

    private final RowMapper<Job> jobRowMapper = (rs, rowNum) -> new Job(
        UUID.fromString(rs.getString("id")),
        rs.getString("schedule"),
        rs.getInt("retriesCount"),
        rs.getInt("maxRetries"),
        rs.getTimestamp("createdAt"),
        rs.getTimestamp("nextRun")
    );

    public JobPolling(JdbcTemplate jdbctemplate, RabbitTemplate rabbittemplate) {
        this.jdbcTemplate = jdbctemplate;
        this.rabbitTemplate = rabbittemplate;
    }

    public static Timestamp getNextRunTime(Timestamp currentTime, String cronString) {

        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        Cron cron = parser.parse(cronString); 

        ZonedDateTime nowUTC = currentTime.toInstant().atZone(ZoneOffset.UTC);
        ZonedDateTime nextUTC = ExecutionTime.forCron(cron).nextExecution(nowUTC).orElse(null);

        if (nextUTC == null) { 
            return null;
        }

        return Timestamp.from(nextUTC.toInstant());
    }

    @Override
    public void run(String...args) {

        while (true) {

            try {
                
                List<Job> jobs = jdbcTemplate.query(
                    "SELECT * FROM dist_jobs_scheduler.jobs WHERE nextRun <= now()", jobRowMapper
                );
                
                for(Job job : jobs) {
        
                    // Add job to the queue 
                    rabbitTemplate.convertAndSend("job.queue", job);

                    // Calculate the next run time 
                    Timestamp nextUTC = getNextRunTime(job.nextRun(), job.Schedule()); 

                    // Insert it into the database
                    try {
                        jdbcTemplate.update(
                        "INSERT INTO jobs (id, schedule, retriesCount, maxRetries, createdAt, nextRun) VALUES (?, ?, ?, ?, ?, ?)",
                        UUID.randomUUID(),
                        request.Schedule(),
                        STARTING_MAX_RETRIES,
                        MAX_RETRIES_LIMIT,
                        job.createdAt(),
                        nextUTC
                    ); 
                    } catch (DataAccessException e) {
                        System.err.println("Failed to update nextRun for job " + job.JobId() + ": " + e.getMessage());
                    }
                }

                // 30-second sleep 
                Thread.sleep(30000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
