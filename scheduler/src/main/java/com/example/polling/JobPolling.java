package com.example.polling;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

import com.example.model.JobDTO;

@Component
public class JobPolling implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate; 
    private final RabbitTemplate rabbitTemplate;

    private static final Logger log = LoggerFactory.getLogger(JobPolling.class);

    private final String DEAD_JOB = "dead";

    private final RowMapper<JobDTO> jobRowMapper = (rs, rowNum) -> new JobDTO(
        UUID.fromString(rs.getString("id")),
        rs.getString("schedule"),
        rs.getInt("retriesCount"),
        rs.getInt("maxRetries"),
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

                try {
                
                    List<JobDTO> jobs = jdbcTemplate.query("SELECT * FROM jobs WHERE nextRun <= now() AND jobActive = 'active'", jobRowMapper);
                    
                    for(JobDTO job : jobs) {

                        if (job.retriesCount() >= job.maxRetries()) {

                            try {
                                // Job will no longer be processed again and put back into the queue. It remains dead forever
                                jdbcTemplate.update(
                                    "UPDATE jobs SET jobActive = ? WHERE id = ?", DEAD_JOB, job.id()
                                );
                            } catch (DataAccessException dbError) {
                                log.error("Database error while handling job {}", job.id(), dbError);
                            }
                        }
                        else {
    
                            Timestamp nextUTC = getNextRunTime(Timestamp.from(Instant.now()), job.schedule());

                            if (nextUTC == null) {
                                log.warn("No next run time for job {} - skipping", job.id());
                                continue; 
                            }
                            
                            try {
                                jdbcTemplate.update(
                                    "UPDATE jobs SET nextRun = ? WHERE id = ?", nextUTC, job.id()
                                );
                            } catch (DataAccessException e) {
                                log.error("Database error while handling job {}", job.id(), e);
                                continue;
                            }

                            rabbitTemplate.convertAndSend("job.queue", job);
                        }
                    }
                } catch (DataAccessException e) {
                    log.error("Database hiccup - trying again next polling cycle", e);
                }

                Thread.sleep(30000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
