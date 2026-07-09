package com.example.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;

import java.sql.Timestamp;
import java.util.UUID;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cronutils.parser.CronParser;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.CronType;
import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;

import com.example.model.JobRequest;

@RestController
@RequestMapping("/job")
public class JobController {

    private static final int MAX_RETRIES_LIMIT = 10;
    private static final int STARTING_RETRIES_COUNT = 0;
    private static final String INITIAL_JOB_STATUS = "active";

    private final JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    public JobController(JdbcTemplate jdbctemplate) {
        this.jdbcTemplate = jdbctemplate; 
    }

    @PostMapping
    public ResponseEntity<String> addJob(@RequestBody JobRequest request) {

        Timestamp nextUTC = getNextRunTime(Timestamp.from(Instant.now()), request.Schedule());

        if (nextUTC == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to calculate next run time...");
        }

        UUID jobId = UUID.randomUUID();
        String schedule = request.Schedule();

        try {
            jdbcTemplate.update(
                "INSERT INTO dist_jobs_scheduler.jobs (id, schedule, retriesCount, maxRetries, nextRun, jobActive) VALUES (?, ?, ?, ?, ?, ?)",
                jobId,
                schedule,
                STARTING_RETRIES_COUNT,
                MAX_RETRIES_LIMIT,
                nextUTC,
                INITIAL_JOB_STATUS
            ); 

            log.info("Added job {} to database with schedule {}", jobId, schedule);

        } catch (DataAccessException e) {
            log.error("Error in adding job {}", jobId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add job");
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