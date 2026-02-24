package com.sentinelops.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommandLogEntry {
    private String id;
    private Instant timestamp;
    private String command;
    private RiskLevel riskLevel;
    private boolean success;
    private int exitCode;
    private String stdout;
    private String stderr;
    private String rollbackSuggestion;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public int getExitCode() { return exitCode; }
    public void setExitCode(int exitCode) { this.exitCode = exitCode; }
    public String getStdout() { return stdout; }
    public void setStdout(String stdout) { this.stdout = stdout; }
    public String getStderr() { return stderr; }
    public void setStderr(String stderr) { this.stderr = stderr; }
    public String getRollbackSuggestion() { return rollbackSuggestion; }
    public void setRollbackSuggestion(String rollbackSuggestion) { this.rollbackSuggestion = rollbackSuggestion; }
}
