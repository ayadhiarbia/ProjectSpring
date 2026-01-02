package com.mycompany.platforme_telemedcine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "consultation")
public class ConsultationProperties {

    private int maxDuration = 60; // minutes
    private int waitingRoomTimeout = 15; // minutes
    private Recording recording = new Recording();
    private List<String> allowedFileTypes = List.of("pdf", "jpg", "jpeg", "png", "doc", "docx");

    public static class Recording {
        private boolean enabled = false;
        private String path = "uploads/recordings";
        private int maxSize = 500; // MB

        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
    }

    // Getters and setters
    public int getMaxDuration() { return maxDuration; }
    public void setMaxDuration(int maxDuration) { this.maxDuration = maxDuration; }

    public int getWaitingRoomTimeout() { return waitingRoomTimeout; }
    public void setWaitingRoomTimeout(int waitingRoomTimeout) { this.waitingRoomTimeout = waitingRoomTimeout; }

    public Recording getRecording() { return recording; }
    public void setRecording(Recording recording) { this.recording = recording; }

    public List<String> getAllowedFileTypes() { return allowedFileTypes; }
    public void setAllowedFileTypes(List<String> allowedFileTypes) { this.allowedFileTypes = allowedFileTypes; }
}