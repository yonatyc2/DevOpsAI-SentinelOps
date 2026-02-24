package com.sentinelops.controller;

import com.sentinelops.model.CommandLogEntry;
import com.sentinelops.model.CommandRiskResult;
import com.sentinelops.model.RiskLevel;
import com.sentinelops.service.CommandExecutionService;
import com.sentinelops.service.CommandHistoryService;
import com.sentinelops.service.CommandExecutionService.ExecuteResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/commands")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*")
public class CommandsController {

    private final CommandExecutionService executionService;
    private final CommandHistoryService historyService;

    public CommandsController(CommandExecutionService executionService, CommandHistoryService historyService) {
        this.executionService = executionService;
        this.historyService = historyService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<CommandRiskResult> analyze(@RequestBody Map<String, String> body) {
        String command = body != null ? body.get("command") : null;
        CommandRiskResult result = executionService.analyze(command);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/execute")
    public ResponseEntity<?> execute(@RequestBody ExecuteRequest request) {
        if (request == null || request.getCommand() == null || request.getCommand().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("executed", false, "rejectionReason", "Command is required."));
        }
        RiskLevel confirmed = request.getConfirmedRiskLevel() != null
                ? request.getConfirmedRiskLevel()
                : RiskLevel.LOW;
        Optional<ExecuteResult> result = executionService.execute(request.getCommand().trim(), confirmed, request.getServerId());
        if (result.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("executed", false, "rejectionReason", "Execution failed."));
        }
        ExecuteResult r = result.get();
        if (!r.isExecuted()) {
            return ResponseEntity.ok(Map.of(
                    "executed", false,
                    "rejectionReason", r.getRejectionReason() != null ? r.getRejectionReason() : "Rejected"));
        }
        return ResponseEntity.ok(Map.of(
                "executed", true,
                "exitCode", r.getExitCode(),
                "stdout", r.getStdout() != null ? r.getStdout() : "",
                "stderr", r.getStderr() != null ? r.getStderr() : "",
                "rollbackSuggestion", r.getRollbackSuggestion() != null ? r.getRollbackSuggestion() : ""));
    }

    @GetMapping("/history")
    public List<CommandLogEntry> history() {
        return historyService.getHistory();
    }

    public static class ExecuteRequest {
        private String command;
        private RiskLevel confirmedRiskLevel;
        private String serverId;

        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        public RiskLevel getConfirmedRiskLevel() { return confirmedRiskLevel; }
        public void setConfirmedRiskLevel(RiskLevel confirmedRiskLevel) { this.confirmedRiskLevel = confirmedRiskLevel; }
        public String getServerId() { return serverId; }
        public void setServerId(String serverId) { this.serverId = serverId; }
    }
}
