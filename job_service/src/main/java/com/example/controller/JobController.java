package com.example.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.util.UUID;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.cronutils.parser.CronParser;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.CronType;
import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;

import com.example.model.Job;
import com.example.model.JobRequest;

import com.example.database.Database;

@RestController
@RequestMapping("/job")
public class JobController {

    private static final int STARTING_MAX_RETRIES = 0;
    private static final int MAX_RETRIES_LIMIT = 10;

    @PostMapping
    public ResponseEntity<String> addJob(JobRequest request) {

        // Calculate the nexttime immediately
        Timestamp nextUTC = getNextRunTime(Timestamp.from(Instant.now()), request.Schedule());

        if (nextUTC == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to add job.");
        }

        // Create job
        Job job = new Job(UUID.randomUUID(), request.Schedule(), STARTING_MAX_RETRIES, MAX_RETRIES_LIMIT, nextUTC);

        // String SQL
        String sql = "INSERT INTO jobs (id, schedule, retriesCount, maxRetries, createdAt, nextRun) VALUES (?, ?, ?, ?, ?, ?)";

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

        return ResponseEntity.status(HttpStatus.CREATED).body("Successfully added job.");
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
}