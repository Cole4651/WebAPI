package com.example.demo.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

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

    private List<String> linesToDelete = new ArrayList<>();

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
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                if (line.contains("Request to /users")) {
                    String timestampString = extractTimeStamp(line);
                    if (timestampString != null) {
                        LocalDateTime timestamp = LocalDateTime.parse(timestampString, DATE_FORMATTER);
                        timestamps.add(timestamp);
                    }
                }
            }

            checkRequestRate(timestamps, lines);
        } catch (IOException e) {
            logger.error("Error reading log file", e);
        }
    }

    private String extractTimeStamp(String logLine) {
        int timestampIndex = logLine.indexOf("Request to /users at ");
        if (timestampIndex != -1) {
            return logLine.substring(timestampIndex + 21).trim();
        }
        return null;
    }

    private void checkRequestRate(List<LocalDateTime> timestamps, List<String> lines) {
        for (int i = 1; i < timestamps.size(); i++) {
            LocalDateTime prevTimestamp = timestamps.get(i - 1);
            LocalDateTime currentTimestamp = timestamps.get(i);

            long secondsDifference = java.time.Duration.between(prevTimestamp, currentTimestamp).getSeconds();

            if (secondsDifference <= 1) {
                System.out.println("Possible DDoS attack between " + prevTimestamp + " and " + currentTimestamp + " (" + secondsDifference + " seconds)");
                markForDeletion(lines, prevTimestamp, currentTimestamp);
            }
        }

        removeProcessedEntriesFromLog(lines);
    }

    private void markForDeletion(List<String> lines, LocalDateTime prevTimestamp, LocalDateTime currentTimestamp) {
        for (String line : lines) {
            String timestampString = extractTimeStamp(line);
            if (timestampString != null) {
                LocalDateTime timestamp = LocalDateTime.parse(timestampString, DATE_FORMATTER);
                if (timestamp.equals(prevTimestamp) || timestamp.equals(currentTimestamp)) {
                    linesToDelete.add(line);  
                }
            }
        }
    }

    private void removeProcessedEntriesFromLog(List<String> lines) {
        List<String> remainingLines = lines.stream()
                                           .filter(line -> !linesToDelete.contains(line))
                                           .collect(Collectors.toList());

        try (FileWriter writer = new FileWriter(LOG_FILE_PATH)) {
            for (String line : remainingLines) {
                writer.write(line + System.lineSeparator()); 
            }
            System.out.println("Log file updated, processed entries removed.");
        } catch (IOException e) {
            logger.error("Error writing to log file", e);
        }
    }
}
