package com.exmaple.processing;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import org.springframework.jdbc.core.RowMapper; 

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cronutils.parser.CronParser;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.CronType;
import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

import com.example.model.JobDTO;
import com.example.transactions.JobHistoryService;

@Component
public class JobProcessing {

    private final JobHistoryService jobHistoryService;

    private static final Logger log = LoggerFactory.getLogger(JobProcessing.class);
    private final Random random = new Random();

    private static final String SUCCESS_JOB = "success";
    private static final String FAILED_JOB = "failed";

    private static final int RETRIES_RESET = 0;

    public JobProcessing(JobHistoryService jobhistoryservice) {
        this.jobHistoryService = jobhistoryservice;
    }

    @RabbitListener(queues = "job.queue")
    public void processJob(JobDTO job) {

        // Need to now add idempotency as well as manual ACKs
        // This ensures no duplicate and jobs are only run at most once 
        
        log.info("Starting job {}", job.id());

        UUID historyId = UUID.randomUUID();
        Timestamp startedTime = Timestamp.from(Instant.now());
        
        try {

            // A job takes about 10 to 40 seconds with a chance for network hiccups to occur 
            int seconds = random.nextInt(31) + 10; 
            for(int i = 0; i < seconds; i++) {
                Thread.sleep(1000L);
                if (random.nextDouble() < 0.01) {
                    throw new RuntimeException("Network Hiccup");
                }
            }

            Timestamp finishedTime = Timestamp.from(Instant.now());

            try {
                jobHistoryService.recordSuccess(historyId, job.id(), SUCCESS_JOB, RETRIES_RESET, startedTime, finishedTime);
            } catch (DataAccessException dbError) {
                log.error("Job {} failed AND couldn't record the success", job.id(), dbError);
            }
                        
            log.info("Job {} finished successfully after {}s", job.id(), seconds);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Job {} was interrupted", job.id(), e);
        }
        catch (RuntimeException e) {
         
            log.error("Job {} failed - incrementing retry counter", job.id(), e);

            try {
                jobHistoryService.recordFailure(historyId, job.id(), FAILED_JOB, startedTime);
            } catch (DataAccessException dbError) {
                log.error("Job {} failed AND couldn't record the failure", job.id(), dbError);
            }
        }
    }
}
