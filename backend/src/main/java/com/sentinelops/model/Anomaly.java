package com.sentinelops.model;

import java.time.Instant;

/**
 * Detected anomaly: disk growth, restart loop, memory trend.
 */
public class Anomaly {
    private String type;       // DISK_GROWTH, RESTART_LOOP, MEMORY_TREND
    private String severity;   // LOW, MEDIUM, HIGH
    private String message;
    private String detail;
    private Instant detectedAt;

    public Anomaly() {}

    public Anomaly(String type, String severity, String message, String detail) {
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.detail = detail;
        this.detectedAt = Instant.now();
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }
}
