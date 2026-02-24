package com.sentinelops.controller;

import com.sentinelops.model.Anomaly;
import com.sentinelops.model.SnapshotHistoryEntry;
import com.sentinelops.model.snapshot.DiskUsage;
import com.sentinelops.model.snapshot.LinuxSnapshot;
import com.sentinelops.service.AnomalyDetectionService;
import com.sentinelops.service.SnapshotHistoryService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*")
public class AnalyticsController {

    private final SnapshotHistoryService historyService;
    private final AnomalyDetectionService anomalyDetectionService;

    public AnalyticsController(SnapshotHistoryService historyService, AnomalyDetectionService anomalyDetectionService) {
        this.historyService = historyService;
        this.anomalyDetectionService = anomalyDetectionService;
    }

    /**
     * Disk usage time series per mount. For building disk growth graph.
     */
    @GetMapping("/disk")
    public Map<String, Object> disk(
            @RequestParam(required = false) String serverId,
            @RequestParam(defaultValue = "50") int limit) {
        List<SnapshotHistoryEntry> entries = historyService.getHistory(serverId, limit);
        Map<String, List<Map<String, Object>>> byMount = new LinkedHashMap<>();
        for (SnapshotHistoryEntry e : entries) {
            if (e.getSnapshot() == null || e.getSnapshot().getLinux() == null) continue;
            LinuxSnapshot linux = e.getSnapshot().getLinux();
            for (DiskUsage d : linux.getDiskUsage()) {
                String mount = d.getMountedOn() != null ? d.getMountedOn() : d.getFilesystem();
                byMount.computeIfAbsent(mount, k -> new ArrayList<>()).add(Map.of(
                        "timestamp", e.getTimestamp().toString(),
                        "usePercent", parseUsePercent(d.getUsePercent())
                ));
            }
        }
        return Map.of("byMount", byMount);
    }

    /**
     * Memory usage time series (used MB over time).
     */
    @GetMapping("/memory")
    public Map<String, Object> memory(
            @RequestParam(required = false) String serverId,
            @RequestParam(defaultValue = "50") int limit) {
        List<SnapshotHistoryEntry> entries = historyService.getHistory(serverId, limit);
        List<Map<String, Object>> dataPoints = entries.stream()
                .filter(e -> e.getSnapshot() != null && e.getSnapshot().getLinux() != null && e.getSnapshot().getLinux().getMemory() != null)
                .map(e -> Map.<String, Object>of(
                        "timestamp", e.getTimestamp().toString(),
                        "memUsedMb", e.getSnapshot().getLinux().getMemory().getMemUsedMb(),
                        "memTotalMb", e.getSnapshot().getLinux().getMemory().getMemTotalMb()
                ))
                .collect(Collectors.toList());
        return Map.of("dataPoints", dataPoints);
    }

    /**
     * Detected anomalies: disk growth, restart loops, memory trend.
     */
    @GetMapping("/anomalies")
    public List<Anomaly> anomalies(
            @RequestParam(required = false) String serverId,
            @RequestParam(defaultValue = "20") int lastN) {
        return anomalyDetectionService.detect(serverId, lastN);
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
