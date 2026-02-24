package com.sentinelops.service;

import com.sentinelops.model.Anomaly;
import com.sentinelops.model.SnapshotHistoryEntry;
import com.sentinelops.model.snapshot.ContainerInfo;
import com.sentinelops.model.snapshot.DiskUsage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple anomaly detection: disk growth, container restart loops, memory trend.
 */
@Service
public class AnomalyDetectionService {

    private final SnapshotHistoryService historyService;

    public AnomalyDetectionService(SnapshotHistoryService historyService) {
        this.historyService = historyService;
    }

    public List<Anomaly> detect(String serverId, int lastN) {
        List<SnapshotHistoryEntry> entries = historyService.getHistory(serverId, lastN);
        List<Anomaly> anomalies = new ArrayList<>();

        if (entries.size() < 2) return anomalies;

        // Disk growth: compare latest vs previous for same mount
        SnapshotHistoryEntry latest = entries.get(entries.size() - 1);
        SnapshotHistoryEntry previous = entries.get(entries.size() - 2);
        if (latest.getSnapshot() != null && latest.getSnapshot().getLinux() != null
                && previous.getSnapshot() != null && previous.getSnapshot().getLinux() != null) {
            List<DiskUsage> curr = latest.getSnapshot().getLinux().getDiskUsage();
            List<DiskUsage> prev = previous.getSnapshot().getLinux().getDiskUsage();
            Map<String, DiskUsage> prevByMount = prev.stream().collect(Collectors.toMap(DiskUsage::getMountedOn, d -> d, (a, b) -> a));
            for (DiskUsage d : curr) {
                DiskUsage p = prevByMount.get(d.getMountedOn());
                if (p != null) {
                    int currPct = parseUsePercent(d.getUsePercent());
                    int prevPct = parseUsePercent(p.getUsePercent());
                    if (currPct >= 90) anomalies.add(new Anomaly("DISK_GROWTH", "HIGH", "Disk " + d.getMountedOn() + " is " + d.getUsePercent() + " full", "Consider cleanup or expansion."));
                    else if (currPct - prevPct >= 10) anomalies.add(new Anomaly("DISK_GROWTH", "MEDIUM", "Disk " + d.getMountedOn() + " grew from " + prevPct + "% to " + currPct + "%", "Monitor for continued growth."));
                }
            }
        }

        // Restart loop: any container with restart count > 3
        if (latest.getSnapshot() != null && latest.getSnapshot().getDocker() != null) {
            for (ContainerInfo c : latest.getSnapshot().getDocker().getContainers()) {
                if (c.getRestartCount() > 3) {
                    anomalies.add(new Anomaly("RESTART_LOOP", "HIGH", "Container " + c.getName() + " has " + c.getRestartCount() + " restarts", "Check logs: docker logs " + c.getName()));
                }
            }
        }

        // Memory trend: compare latest vs previous mem used
        if (latest.getSnapshot() != null && latest.getSnapshot().getLinux() != null && latest.getSnapshot().getLinux().getMemory() != null
                && previous.getSnapshot() != null && previous.getSnapshot().getLinux() != null && previous.getSnapshot().getLinux().getMemory() != null) {
            long currUsed = latest.getSnapshot().getLinux().getMemory().getMemUsedMb();
            long prevUsed = previous.getSnapshot().getLinux().getMemory().getMemUsedMb();
            long currTotal = latest.getSnapshot().getLinux().getMemory().getMemTotalMb();
            if (currTotal > 0 && currUsed > prevUsed && (currUsed - prevUsed) >= currTotal * 0.05) {
                anomalies.add(new Anomaly("MEMORY_TREND", "MEDIUM", "Memory usage increased by " + (currUsed - prevUsed) + " MB", "Possible memory leak or load increase."));
            }
        }

        return anomalies;
    }

    private static int parseUsePercent(String s) {
        if (s == null) return 0;
        try {
            return Integer.parseInt(s.replace("%", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
