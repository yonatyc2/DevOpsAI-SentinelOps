package com.sentinelops.service;

import com.sentinelops.model.CommandRiskResult;
import com.sentinelops.model.RiskLevel;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Analyzes shell/SQL commands for risk and suggests rollback where applicable.
 * Detects destructive patterns: rm -rf, LVM, disk, database drop, etc.
 */
@Service
public class CommandRiskAnalyzer {

    // High risk: often irreversible or system-wide
    private static final Pattern[] HIGH_PATTERNS = {
            Pattern.compile("\\brm\\s+-rf\\s+/", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\brm\\s+-rf\\s+\\*/", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\blvreduce\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bpvremove\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bvgremove\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\blvremove\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bmkfs\\.", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdd\\s+.*if=.*of=/dev/", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdrop\\s+database\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\btruncate\\s+(table\\s+)?\\w+", Pattern.CASE_INSENSITIVE),
            Pattern.compile(">\\s*/dev/sd[a-z]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\breboot\\b|\\bshutdown\\s+-h\\s+now\\b", Pattern.CASE_INSENSITIVE),
    };

    // Medium risk: reversible but impactful
    private static final Pattern[] MEDIUM_PATTERNS = {
            Pattern.compile("\\brm\\s+-rf\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bkill\\s+-9\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsystemctl\\s+stop\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsystemctl\\s+restart\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdocker\\s+rm\\s+-f\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdocker\\s+stop\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdelete\\s+from\\s+\\w+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdrop\\s+table\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bALTER\\s+TABLE\\s+\\w+\\s+DROP\\b", Pattern.CASE_INSENSITIVE),
    };

    public CommandRiskResult analyze(String command) {
        if (command == null || command.isBlank()) {
            return new CommandRiskResult(RiskLevel.LOW, "Empty command.", "");
        }
        String normalized = command.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);

        for (Pattern p : HIGH_PATTERNS) {
            if (p.matcher(normalized).find()) {
                String reason = describeHighRisk(normalized, p);
                String rollback = rollbackForHigh(normalized, lower);
                return new CommandRiskResult(RiskLevel.HIGH, reason, rollback);
            }
        }
        for (Pattern p : MEDIUM_PATTERNS) {
            if (p.matcher(normalized).find()) {
                String reason = describeMediumRisk(normalized, p);
                String rollback = rollbackForMedium(normalized, lower);
                return new CommandRiskResult(RiskLevel.MEDIUM, reason, rollback);
            }
        }

        return new CommandRiskResult(RiskLevel.LOW,
                "No high- or medium-risk patterns detected. Command appears read-only or low impact.",
                "No rollback needed for low-risk command.");
    }

    private static String describeHighRisk(String cmd, Pattern p) {
        String pat = p.pattern();
        if (pat.contains("rm") && pat.contains("-rf")) return "Recursive force delete (rm -rf) is destructive and can remove system or data.";
        if (pat.contains("lvreduce")) return "LVM lvreduce can cause data loss if the filesystem is not resized first.";
        if (pat.contains("pvremove") || pat.contains("vgremove") || pat.contains("lvremove")) return "LVM remove operations can destroy volumes and data.";
        if (pat.contains("mkfs")) return "Formatting a block device erases all data on it.";
        if (pat.contains("dd")) return "dd to a block device overwrites disk contents irreversibly.";
        if (pat.contains("drop") && pat.contains("database")) return "Dropping a database removes all data permanently.";
        if (pat.contains("truncate")) return "TRUNCATE removes all rows from a table without row-by-row delete.";
        if (pat.contains("/dev/sd")) return "Writing to a raw block device can destroy filesystems and data.";
        if (pat.contains("reboot") || pat.contains("shutdown")) return "Reboot or shutdown affects the entire system.";
        return "Command matches a high-risk pattern. Verify target and impact before running.";
    }

    private static String describeMediumRisk(String cmd, Pattern p) {
        String pat = p.pattern();
        if (pat.contains("rm") && pat.contains("-rf")) return "Recursive delete (rm -rf) can remove large amounts of data.";
        if (pat.contains("kill") && pat.contains("-9")) return "SIGKILL (-9) forcibly terminates processes without cleanup.";
        if (pat.contains("systemctl")) return "Stopping or restarting services can cause downtime.";
        if (pat.contains("docker")) return "Stopping or removing containers affects running workloads.";
        if (pat.contains("delete") || pat.contains("drop table") || pat.contains("ALTER")) return "SQL delete/drop alters or removes data.";
        return "Command has medium risk. Confirm target and have a rollback plan.";
    }

    private static String rollbackForHigh(String cmd, String lower) {
        if (lower.contains("lvreduce")) return "LVM: Cannot undo lvreduce. Ensure you have backups; use lvextend to grow again if space is available.";
        if (lower.contains("pvremove") || lower.contains("vgremove") || lower.contains("lvremove")) return "LVM remove is irreversible. Restore from backup if data was on the LV.";
        if (lower.contains("drop database")) return "Restore database from a recent backup (pg_restore or dump).";
        if (lower.contains("truncate")) return "Restore table data from backup or point-in-time recovery.";
        if (lower.contains("reboot") || lower.contains("shutdown")) return "System will come back up after reboot; ensure services start on boot or start them manually.";
        return "No automated rollback. Restore from backups if data was affected.";
    }

    private static String rollbackForMedium(String cmd, String lower) {
        if (lower.contains("systemctl stop")) return "Run: systemctl start <service> to bring the service back up.";
        if (lower.contains("docker stop")) return "Run: docker start <container> to start the container again.";
        if (lower.contains("docker rm")) return "Container is removed; recreate from image if needed (docker run ...).";
        if (lower.contains("delete from") || lower.contains("drop table")) return "Restore affected rows or table from backup or transaction log if available.";
        return "Reverse the action manually (e.g. restart service, restore from backup).";
    }
}
