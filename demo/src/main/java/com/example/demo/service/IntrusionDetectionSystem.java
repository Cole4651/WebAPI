package com.example.demo.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class IntrusionDetectionSystem {
    private static final Logger logger = LoggerFactory.getLogger(IntrusionDetectionSystem.class);
    private static final String LOG_FILE_PATH = "demo/logs/application-log.txt";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostConstruct
    public void startMonitoring() {
        try {
            System.out.println("Starting to monitor request rates.");

            if (!Files.exists(Paths.get(LOG_FILE_PATH))) {
                logger.error("Log file does not exist at path: {}", LOG_FILE_PATH);
                return;
            }
        } catch (Exception e) {
            logger.error("Error during intrusion detection system initialization", e);
        }
    }

    @Scheduled(fixedRate = 5000)
    public void monitorRequestRate() {
        List<LocalDateTime> timestamps = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Request to /users")) {
                    String timestampString = extractTimeStamp(line);
                    if (timestampString != null) {
                        LocalDateTime timestamp = LocalDateTime.parse(timestampString, DATE_FORMATTER);
                        timestamps.add(timestamp);
                        System.out.println("Extracted timestamp: " + timestampString);
                    }
                }
            }
            System.out.println("Total timestamps found: " + timestamps.size());
            checkRequestRate(timestamps);
        } catch (IOException e) {
            logger.error("Error reading log file", e);
        }
    }

    // Method to extract timestamp from the log line
    private String extractTimeStamp(String logLine) {
        int timestampIndex = logLine.indexOf("at ");
        if (timestampIndex != -1) {
            return logLine.substring(timestampIndex + 3).trim(); // Ensure no extra spaces
        }
        return null;
    }

    // Method to check if requests are made at a high rate (1 per second or faster)
    private void checkRequestRate(List<LocalDateTime> timestamps) {
        for (int i = 1; i < timestamps.size(); i++) {
            LocalDateTime prevTimestamp = timestamps.get(i - 1);
            LocalDateTime currentTimestamp = timestamps.get(i);

            // Calculate the difference in seconds between the current and previous request
            long secondsDifference = java.time.Duration.between(prevTimestamp, currentTimestamp).getSeconds();
            System.out.println("Checking request rate: " + prevTimestamp + " -> " + currentTimestamp + " (" + secondsDifference + " seconds)");

            // Check if the difference is 1 second or less
            if (secondsDifference <= 1) {
                System.out.println("High request rate detected between " + prevTimestamp + " and " + currentTimestamp + " (" + secondsDifference + " seconds)");
            }
        }
    }
}
