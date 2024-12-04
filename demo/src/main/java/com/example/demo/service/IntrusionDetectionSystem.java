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

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class IntrusionDetectionSystem {
    private static final Logger logger = LoggerFactory.getLogger(IntrusionDetectionSystem.class);
    private static final String LOG_FILE_PATH = "logs/application-log.txt";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    // This method will be called after Spring initializes the bean
    @PostConstruct
    public void startMonitoring() {
        try {
            // Log the start of monitoring
            System.out.println("Starting to monitor request rates.");

            // Check if the log file exists
            if (!Files.exists(Paths.get(LOG_FILE_PATH))) {
                logger.error("Log file does not exist at path: {}", LOG_FILE_PATH);
                return;  // Exit if the file doesn't exist
            }
            monitorRequestRate();
        } catch (Exception e) {
            // Log any initialization failures
            logger.error("Error during intrusion detection system initialization", e);
        }
    }

    // Method to monitor the request rate by reading the log file
    public void monitorRequestRate() {
        List<LocalDateTime> timestamps = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Only consider lines that have the desired request (to /users)
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
        int timestampIndex = logLine.lastIndexOf("at ");
        if (timestampIndex != -1) {
            return logLine.substring(timestampIndex + 3);
        }
        return null;
    }

    // Method to check if requests are made at a high rate (1 per second or faster)
    private void checkRequestRate(List<LocalDateTime> timestamps) {
        for (int i = 1; i < timestamps.size(); i++) {
            LocalDateTime prevTimestamp = timestamps.get(i - 1);
            LocalDateTime currentTimestamp = timestamps.get(i);

            // Check if requests are made 1 second or faster
            if (currentTimestamp.minusSeconds(1).isBefore(prevTimestamp) || currentTimestamp.equals(prevTimestamp)) {
                System.out.println("High request rate detected between " + prevTimestamp + " and " + currentTimestamp);
            }
        }
    }
}
