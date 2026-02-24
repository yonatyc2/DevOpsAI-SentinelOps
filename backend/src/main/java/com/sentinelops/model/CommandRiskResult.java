package com.sentinelops.model;

/**
 * Result of command risk analysis: level, reason, and optional rollback suggestion.
 */
public class CommandRiskResult {
    private final RiskLevel riskLevel;
    private final String reason;
    private final String rollbackSuggestion;

    public CommandRiskResult(RiskLevel riskLevel, String reason, String rollbackSuggestion) {
        this.riskLevel = riskLevel;
        this.reason = reason;
        this.rollbackSuggestion = rollbackSuggestion != null ? rollbackSuggestion : "";
    }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public String getReason() { return reason; }
    public String getRollbackSuggestion() { return rollbackSuggestion; }
}
