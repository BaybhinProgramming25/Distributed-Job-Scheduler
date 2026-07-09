package com.example.transactions;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

import com.example.model.JobDTO;

@Service
public class JobHistoryService {

    private final JdbcTemplate jdbcTemplate;

    public JobHistoryService(JdbcTemplate jdbctemplate) {
        this.jdbcTemplate = jdbctemplate;
    }

    @Transactional
    public void recordFailure(UUID historyId, UUID jobId, String failedJob, Timestamp startedTime) {

        jdbcTemplate.update(
            "INSERT INTO dist_jobs_scheduler.history (id, jobId, jobStatus, jobStarted) VALUES (?, ?, ?, ?)",
            historyId,
            jobId,
            failedJob,
            startedTime
        );

        // Also need to increment the counter 
        jdbcTemplate.update(
            "UPDATE dist_jobs_scheduler.jobs SET retriesCount = retriesCount + 1 WHERE id = ?", jobId
        );
    }

    @Transactional
    public void recordSuccess(UUID historyId, UUID jobId, String successJob, int retriesReset, Timestamp startedTime, Timestamp finishedTime) {

        jdbcTemplate.update(
            "INSERT INTO dist_jobs_scheduler.history (id, jobId, jobStatus, jobStarted, jobFinished) VALUES (?, ?, ?, ?, ?)",
            historyId,
            jobId,
            successJob,
            startedTime,
            finishedTime
        );

        jdbcTemplate.update(
            "UPDATE dist_jobs_scheduler.jobs SET retriesCount = ? WHERE id = ?", retriesReset, jobId
        );

    }

    @Transactional
    public boolean claimExecution(UUID jobId, Timestamp executionTime) {

        String idem_key = jobId.toString() + "|" + executionTime.toInstant().toString();

        try {

            jdbcTemplate.update(
                "INSERT INTO dist_jobs_scheduler.processedJobs (idempotency_key) VALUES (?)", idem_key
            );

            return true; // The key isn't taken, return true
        } catch (DuplicateKeyException e) {
            return false; // The key is already taken
        }
    }
}