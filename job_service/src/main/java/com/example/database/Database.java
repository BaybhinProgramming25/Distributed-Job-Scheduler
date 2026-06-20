package com.example.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

import io.github.cdimascio.dotenv.Dotenv;

public class Database {

    private static final HikariDataSource dataSource;

    static {  
        HikariConfig config = new HikariConfig();
        Dotenv dotenv = Dotenv.load();
        config.setJdbcUrl(dotenv.get("DB_CONN_STRING"));
        config.setUsername(dotenv.get("DB_USER"));
        config.setPassword(dotenv.get("DB_PASSWORD"));         
        config.setMaximumPoolSize(10);  
        dataSource = new HikariDataSource(config);
    }

    public static DataSource getDataSource() {
        return dataSource;
    }
}