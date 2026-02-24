package com.sentinelops.service;

import com.sentinelops.model.CommandRiskResult;
import com.sentinelops.model.RiskLevel;
import com.sentinelops.service.SshExecutionService.SshCommandResult;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orchestrates command risk analysis, confirmation check, SSH execution, and history logging.
 */
@Service
public class CommandExecutionService {

    private final CommandRiskAnalyzer riskAnalyzer;
    private final SshExecutionService sshExecutionService;
    private final CommandHistoryService historyService;

    public CommandExecutionService(CommandRiskAnalyzer riskAnalyzer,
                                    SshExecutionService sshExecutionService,
                                    CommandHistoryService historyService) {
        this.riskAnalyzer = riskAnalyzer;
        this.sshExecutionService = sshExecutionService;
        this.historyService = historyService;
    }

    public CommandRiskResult analyze(String command) {
        return riskAnalyzer.analyze(command);
    }

    /**
     * Execute command after confirmation. For HIGH or MEDIUM risk, confirmedRiskLevel must match
     * the current analysis; otherwise execution is refused.
     * @param serverId optional server to run on; null uses default SSH config
     */
    public Optional<ExecuteResult> execute(String command, RiskLevel confirmedRiskLevel, String serverId) {
        if (command == null || command.isBlank()) {
            return Optional.empty();
        }
        String trimmed = command.trim();
        CommandRiskResult analysis = riskAnalyzer.analyze(trimmed);

        if (analysis.getRiskLevel() == RiskLevel.HIGH || analysis.getRiskLevel() == RiskLevel.MEDIUM) {
            if (confirmedRiskLevel != analysis.getRiskLevel()) {
                return Optional.of(ExecuteResult.rejected(
                        "Confirmation level does not match analyzed risk. Analyzed: " + analysis.getRiskLevel()
                                + ", confirmed: " + confirmedRiskLevel + ". Re-run analysis and confirm with the shown risk level."));
            }
        }

        Optional<SshCommandResult> result = serverId != null && !serverId.isBlank()
                ? sshExecutionService.executeWithServer(serverId, trimmed)
                : sshExecutionService.execute(trimmed);
        if (result.isEmpty()) {
            return Optional.of(ExecuteResult.rejected("SSH not configured or connection failed."));
        }
        SshCommandResult r = result.get();
        String rollback = analysis.getRollbackSuggestion();
        historyService.append(trimmed, analysis.getRiskLevel(), r.isSuccess(), r.getExitCode(),
                r.getStdout(), r.getStderr(), rollback);

        return Optional.of(new ExecuteResult(true, r.getExitCode(), r.getStdout(), r.getStderr(), rollback));
    }

    public static class ExecuteResult {
        private final boolean executed;
        private final String rejectionReason;
        private final int exitCode;
        private final String stdout;
        private final String stderr;
        private final String rollbackSuggestion;

        public ExecuteResult(boolean executed, int exitCode, String stdout, String stderr, String rollbackSuggestion) {
            this.executed = executed;
            this.rejectionReason = null;
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.rollbackSuggestion = rollbackSuggestion;
        }

        private ExecuteResult(String rejectionReason) {
            this.executed = false;
            this.rejectionReason = rejectionReason;
            this.exitCode = -1;
            this.stdout = null;
            this.stderr = null;
            this.rollbackSuggestion = null;
        }

        public static ExecuteResult rejected(String reason) {
            return new ExecuteResult(reason);
        }

        public boolean isExecuted() { return executed; }
        public String getRejectionReason() { return rejectionReason; }
        public int getExitCode() { return exitCode; }
        public String getStdout() { return stdout; }
        public String getStderr() { return stderr; }
        public String getRollbackSuggestion() { return rollbackSuggestion; }
    }
}
