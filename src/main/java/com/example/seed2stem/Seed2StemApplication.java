package com.example.seed2stem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class Seed2StemApplication {

    public static void main(String[] args) {
        // Honour an explicit timezone env var; fall back to Eastern Time.
        // Set APP_TIMEZONE=America/New_York (or your zone) in Elastic Beanstalk
        // environment configuration to keep all LocalDateTime.now() calls correct.
        String tz = System.getenv("APP_TIMEZONE");
        TimeZone.setDefault(TimeZone.getTimeZone(tz != null ? tz : "America/New_York"));

        SpringApplication.run(Seed2StemApplication.class, args);
    }

}
