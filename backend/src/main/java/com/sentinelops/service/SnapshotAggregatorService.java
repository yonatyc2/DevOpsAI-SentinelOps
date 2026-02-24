package com.sentinelops.service;

import com.sentinelops.model.snapshot.SystemSnapshot;
import org.springframework.stereotype.Service;

/**
 * Builds a full system snapshot by calling Linux, Docker, and Postgres snapshot services.
 */
@Service
public class SnapshotAggregatorService {

    private final LinuxSnapshotService linuxSnapshotService;
    private final DockerSnapshotService dockerSnapshotService;
    private final PostgresSnapshotService postgresSnapshotService;

    public SnapshotAggregatorService(LinuxSnapshotService linuxSnapshotService,
                                    DockerSnapshotService dockerSnapshotService,
                                    PostgresSnapshotService postgresSnapshotService) {
        this.linuxSnapshotService = linuxSnapshotService;
        this.dockerSnapshotService = dockerSnapshotService;
        this.postgresSnapshotService = postgresSnapshotService;
    }

    public SystemSnapshot capture() {
        return capture(null);
    }

    public SystemSnapshot capture(String serverId) {
        SystemSnapshot snapshot = new SystemSnapshot();
        snapshot.setLinux(linuxSnapshotService.capture(serverId));
        snapshot.setDocker(dockerSnapshotService.capture(serverId));
        snapshot.setPostgres(postgresSnapshotService.capture(serverId));
        return snapshot;
    }
}
