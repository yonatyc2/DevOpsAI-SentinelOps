package com.sentinelops.controller;

import com.sentinelops.model.snapshot.LinuxSnapshot;
import com.sentinelops.model.snapshot.SystemSnapshot;
import com.sentinelops.service.SnapshotAggregatorService;
import com.sentinelops.service.SnapshotHistoryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/snapshot")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*")
public class SnapshotController {

    private final SnapshotAggregatorService snapshotAggregatorService;
    private final SnapshotHistoryService snapshotHistoryService;

    public SnapshotController(SnapshotAggregatorService snapshotAggregatorService,
                              SnapshotHistoryService snapshotHistoryService) {
        this.snapshotAggregatorService = snapshotAggregatorService;
        this.snapshotHistoryService = snapshotHistoryService;
    }

    @GetMapping
    public SystemSnapshot getSnapshot(@RequestParam(required = false) String serverId) {
        try {
            SystemSnapshot snapshot = snapshotAggregatorService.capture(serverId);
            snapshotHistoryService.append(serverId, snapshot);
            return snapshot;
        } catch (Exception e) {
            SystemSnapshot fallback = new SystemSnapshot();
            LinuxSnapshot linux = new LinuxSnapshot();
            linux.setError("Snapshot failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
                    + ". Check SSH config or select a configured server.");
            fallback.setLinux(linux);
            return fallback;
        }
    }
}
