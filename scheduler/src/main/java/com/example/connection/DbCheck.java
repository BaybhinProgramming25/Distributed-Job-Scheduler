package com.example.connection;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DbCheck implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DbCheck(JdbcTemplate jdbctemplate) {
        this.jdbcTemplate = jdbctemplate;
    }

    @Override
    public void run(String... args) {
        Integer ok = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        System.out.println("Connected to CockroachDB! SELECT 1 => " + ok);
    }
}