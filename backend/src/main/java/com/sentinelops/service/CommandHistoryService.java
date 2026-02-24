package com.sentinelops.service;

import com.sentinelops.model.CommandLogEntry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory command execution history for audit. Phase 4+ can persist to DB.
 */
@Service
public class CommandHistoryService {

    private final List<CommandLogEntry> history = new CopyOnWriteArrayList<>();
    private static final int MAX_ENTRIES = 500;

    public CommandLogEntry append(String command, com.sentinelops.model.RiskLevel riskLevel,
                                   boolean success, int exitCode, String stdout, String stderr,
                                   String rollbackSuggestion) {
        CommandLogEntry entry = new CommandLogEntry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTimestamp(Instant.now());
        entry.setCommand(command);
        entry.setRiskLevel(riskLevel);
        entry.setSuccess(success);
        entry.setExitCode(exitCode);
        entry.setStdout(stdout);
        entry.setStderr(stderr);
        entry.setRollbackSuggestion(rollbackSuggestion);
        history.add(entry);
        while (history.size() > MAX_ENTRIES) {
            history.remove(0);
        }
        return entry;
    }

    public List<CommandLogEntry> getHistory() {
        return new ArrayList<>(history);
    }
}
