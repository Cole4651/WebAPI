package com.example.demo.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntrusionDetectionSystem {
    private static final Logger logger = LoggerFactory.getLogger(IntrusionDetectionSystem.class);
    private static final String LOG_FILE_PATH = "/logs/application-log.txt";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void monitorRequestRate() {
        System.out.println("String to monitor request rates. ");
        List<LocalDateTime> timestamps = new ArrayList<>();

        try(BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE_PATH))) {
            String line;
            while((line = reader.readLine()) != null) {
                if(line.contains("Request to /users")) {
                    String timestampString = extractTimeStamp(line);
                    LocalDateTime timestamp = LocalDateTime.parse(timestampString, DATE_FORMATTER);
                    timestamps.add(timestamp);
                    System.out.println("Extracted timestamp: " + timestampString);
                }
            }
            System.out.println("Total timestamps found: " + timestamps.size());
            checkRequestRate(timestamps);
        } catch (IOException e) {
            logger.error("Error reading log file", e);
        }
    }

    private String extractTimeStamp(String logLine) {
        int timestampIndex = logLine.lastIndexOf("at ");
        if(timestampIndex != -1) {
            return logLine.substring(timestampIndex + 3);
        }
        return null;
        
    }

    private void checkRequestRate(List<LocalDateTime> timestamps) {
        for(int i = 1; i < timestamps.size(); i++) {
            LocalDateTime prevTimestamp = timestamps.get(i - 1);
            LocalDateTime currentTimestamp = timestamps.get(i);

            if(currentTimestamp.minusSeconds(1).isBefore(prevTimestamp) || currentTimestamp.equals(prevTimestamp)) {
                System.out.println("High request rate detected.");
            }
        }
    }

    public static void main(String[] args) {
        IntrusionDetectionSystem monitor = new IntrusionDetectionSystem();
        monitor.monitorRequestRate();
    }
}
