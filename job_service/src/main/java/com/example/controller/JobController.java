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
    private final JdbcTemplate jdbcTemplate;

    public JobController(JdbcTemplate jdbctemplate) {
        this.jdbcTemplate = jdbctemplate; 
    }

    @PostMapping
    public ResponseEntity<String> addJob(@RequestBody JobRequest request) {

        Timestamp nextUTC = getNextRunTime(Timestamp.from(Instant.now()), request.Schedule());

        if (nextUTC == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to calculate next run time...");
        }

        try {
            jdbcTemplate.update(
            "INSERT INTO jobs (id, schedule, maxRetries, nextRun) VALUES (?, ?, ?, ?)",
            UUID.randomUUID(),
            request.Schedule(),
            MAX_RETRIES_LIMIT,
            nextUTC
        ); 
        } catch (DataAccessException e) {
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