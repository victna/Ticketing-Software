package com.ticketing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@EnableScheduling
public class TicketingApplication {

    public static void main(String[] args) throws IOException {
        // SQLite creates the database file itself but not its folder
        Files.createDirectories(Path.of("data"));
        SpringApplication.run(TicketingApplication.class, args);
    }
}
