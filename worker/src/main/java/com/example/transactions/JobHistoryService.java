package com.example.transactions;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.jdbc.core.JdbcTemplate;

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
            "INSERT INTO history (id, jobId, jobStatus, jobStarted) VALUES (?, ?, ?, ?)",
            historyId,
            jobId,
            failedJob,
            startedTime
        );

        // Also need to increment the counter 
        jdbcTemplate.update(
            "UPDATE jobs SET retriesCount = retriesCount + 1 WHERE id = ?", jobId
        );
    }

    @Transactional
    public void recordSuccess(UUID historyId, UUID jobId, String successJob, int retriesReset, Timestamp startedTime, Timestamp finishedTime) {

        jdbcTemplate.update(
            "INSERT INTO history (id, jobId, jobStatus, jobStarted, jobFinished) VALUES (?, ?, ?, ?, ?)",
            historyId,
            jobId,
            successJob,
            startedTime,
            finishedTime
        );

        jdbcTemplate.update(
            "UPDATE jobs SET retriesCount = ? WHERE id = ?", retriesReset, jobId
        );

    }
}