package com.sentinelops.model;

import com.sentinelops.model.snapshot.SystemSnapshot;

import java.time.Instant;

/**
 * A snapshot plus server and timestamp for history/analytics.
 */
public class SnapshotHistoryEntry {
    private Instant timestamp;
    private String serverId;
    private SystemSnapshot snapshot;

    public SnapshotHistoryEntry() {}

    public SnapshotHistoryEntry(Instant timestamp, String serverId, SystemSnapshot snapshot) {
        this.timestamp = timestamp;
        this.serverId = serverId;
        this.snapshot = snapshot;
    }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }
    public SystemSnapshot getSnapshot() { return snapshot; }
    public void setSnapshot(SystemSnapshot snapshot) { this.snapshot = snapshot; }
}
