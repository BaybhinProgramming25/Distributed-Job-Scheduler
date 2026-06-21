package com.example.polling;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;

@Component
public class JobPolling {

    private final JdbcTemplate jdbcTemplate; 

    public JobPolling(JdbcTemplate jdbctemplate) {
        this.jdbcTemplate = jdbctemplate;
    }

    public void pollJobs() {
        /*
        Left to do:
        - Add jobs to the database from job_service
        - Poll for jobs that are on the database
        - Push ready jobs onto the RabbitMQ
        */
        
        while (true) {

            // Poll the database 
            try {
                
                List<Map<String, Object>> jobs = jdbcTemplate.queryForObject(
                    "SELECT * FROM dist_jobs_scheduler.jobs WHERE nextRun <= now()"
                );

                for (Map<String, Object> job : jobs) {
                    System.out.println(job);
                }

                // Do some processing here afterwards...
                

                // Sleep for 5 minutes
                Thread.sleep(5 * 60 * 1000);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
