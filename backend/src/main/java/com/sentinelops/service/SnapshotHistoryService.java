package com.sentinelops.service;

import com.sentinelops.model.SnapshotHistoryEntry;
import com.sentinelops.model.snapshot.SystemSnapshot;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * In-memory store of recent snapshots for analytics and trend detection.
 * Keeps last N entries per server (or global). Max 200 entries total.
 */
@Service
public class SnapshotHistoryService {

    private static final int MAX_ENTRIES = 200;

    private final List<SnapshotHistoryEntry> history = new CopyOnWriteArrayList<>();

    public void append(String serverId, SystemSnapshot snapshot) {
        history.add(new SnapshotHistoryEntry(Instant.now(), serverId, snapshot));
        while (history.size() > MAX_ENTRIES) {
            history.remove(0);
        }
    }

    public List<SnapshotHistoryEntry> getHistory() {
        return new ArrayList<>(history);
    }

    public List<SnapshotHistoryEntry> getHistory(String serverId, int limit) {
        List<SnapshotHistoryEntry> filtered = history.stream()
                .filter(e -> serverId == null || serverId.equals(e.getServerId()))
                .collect(Collectors.toList());
        int from = Math.max(0, filtered.size() - limit);
        return filtered.subList(from, filtered.size());
    }
}
