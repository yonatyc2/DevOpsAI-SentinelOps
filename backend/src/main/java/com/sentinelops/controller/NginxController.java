package com.sentinelops.controller;

import com.sentinelops.service.NginxSnapshotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nginx")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*")
public class NginxController {

    private final NginxSnapshotService nginxSnapshotService;

    public NginxController(NginxSnapshotService nginxSnapshotService) {
        this.nginxSnapshotService = nginxSnapshotService;
    }

    @GetMapping("/ussd-logs")
    public ResponseEntity<Map<String, Object>> ussdLogs(
            @RequestParam(required = false) String serverId,
            @RequestParam(defaultValue = "80") int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 500));
        List<String> lines = nginxSnapshotService.captureUssdLogLines(serverId, boundedLimit);
        return ResponseEntity.ok(Map.of(
                "serverId", serverId != null ? serverId : "",
                "limit", boundedLimit,
                "count", lines.size(),
                "fetchedAt", Instant.now().toString(),
                "lines", lines
        ));
    }
}
