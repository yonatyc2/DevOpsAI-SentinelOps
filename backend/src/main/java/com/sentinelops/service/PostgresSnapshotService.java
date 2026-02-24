package com.sentinelops.service;

import com.sentinelops.config.PostgresProperties;
import com.sentinelops.model.snapshot.PostgresSnapshot;
import com.sentinelops.service.SshExecutionService.SshCommandResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Collects PostgreSQL snapshot via SSH (psql on remote host): activity, sizes, locks.
 */
@Service
public class PostgresSnapshotService {

    private final SshExecutionService sshExecutionService;
    private final PostgresProperties postgresProperties;

    public PostgresSnapshotService(SshExecutionService sshExecutionService, PostgresProperties postgresProperties) {
        this.sshExecutionService = sshExecutionService;
        this.postgresProperties = postgresProperties;
    }

    public PostgresSnapshot capture() {
        return capture(null);
    }

    public PostgresSnapshot capture(String serverId) {
        PostgresSnapshot snapshot = new PostgresSnapshot();
        if (!postgresProperties.isEnabled()) {
            return snapshot;
        }
        String psqlCmd = String.format("psql -h %s -p %d -U %s -d %s -t -A",
                postgresProperties.getHost(), postgresProperties.getPort(),
                postgresProperties.getUser(), postgresProperties.getDatabase());

        Optional<SshCommandResult> activeResult = (serverId != null ? sshExecutionService.executeWithServer(serverId, psqlCmd + " -c \"SELECT count(*) FROM pg_stat_activity WHERE state = 'active';\" 2>/dev/null") : sshExecutionService.execute(
                psqlCmd + " -c \"SELECT count(*) FROM pg_stat_activity WHERE state = 'active';\" 2>/dev/null"));
        if (activeResult.isPresent() && activeResult.get().isSuccess()) {
            try {
                snapshot.setActiveConnections(Integer.parseInt(activeResult.get().getStdout().trim()));
            } catch (NumberFormatException ignored) {}
        }

        String sizesCmd = psqlCmd + " -c \"SELECT datname, pg_size_pretty(pg_database_size(datname)) FROM pg_database ORDER BY pg_database_size(datname) DESC;\" 2>/dev/null";
        Optional<SshCommandResult> sizesResult = serverId != null ? sshExecutionService.executeWithServer(serverId, sizesCmd) : sshExecutionService.execute(sizesCmd);
        if (sizesResult.isPresent() && sizesResult.get().isSuccess()) {
            List<PostgresSnapshot.DatabaseSize> sizes = new ArrayList<>();
            for (String line : sizesResult.get().getStdout().split("\n")) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 2) {
                    PostgresSnapshot.DatabaseSize ds = new PostgresSnapshot.DatabaseSize();
                    ds.setName(parts[0].trim());
                    ds.setSize(parts[1].trim());
                    sizes.add(ds);
                }
            }
            snapshot.setDatabaseSizes(sizes);
        }

        String locksCmd = psqlCmd + " -c \"SELECT count(*), mode FROM pg_locks GROUP BY mode;\" 2>/dev/null";
        Optional<SshCommandResult> locksResult = serverId != null ? sshExecutionService.executeWithServer(serverId, locksCmd) : sshExecutionService.execute(locksCmd);
        if (locksResult.isPresent() && locksResult.get().isSuccess() && !locksResult.get().getStdout().isBlank()) {
            snapshot.setLocksSummary(locksResult.get().getStdout().trim());
        }

        String slowCmd = psqlCmd + " -c \"SELECT count(*) FROM pg_stat_activity WHERE state = 'active' AND query_start < now() - interval '5 seconds';\" 2>/dev/null";
        Optional<SshCommandResult> slowResult = serverId != null ? sshExecutionService.executeWithServer(serverId, slowCmd) : sshExecutionService.execute(slowCmd);
        if (slowResult.isPresent() && slowResult.get().isSuccess()) {
            try {
                int slow = Integer.parseInt(slowResult.get().getStdout().trim());
                snapshot.setSlowQueriesSummary(slow + " queries running longer than 5s");
            } catch (NumberFormatException ignored) {}
        }

        if (activeResult.isPresent() && !activeResult.get().isSuccess() && snapshot.getError() == null) {
            snapshot.setError("Postgres unreachable or psql not available: " + activeResult.get().getStderr().trim());
        }
        return snapshot;
    }
}
