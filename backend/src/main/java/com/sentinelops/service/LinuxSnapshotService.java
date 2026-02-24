package com.sentinelops.service;

import com.sentinelops.model.snapshot.DiskUsage;
import com.sentinelops.model.snapshot.LinuxSnapshot;
import com.sentinelops.model.snapshot.MemoryInfo;
import com.sentinelops.model.snapshot.UptimeInfo;
import com.sentinelops.service.SshExecutionService.SshCommandResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects Linux system data via SSH: df -h, free -m, uptime.
 * Parses output into structured LinuxSnapshot.
 */
@Service
public class LinuxSnapshotService {

    private final SshExecutionService sshExecutionService;

    // df -h: Filesystem Size Used Avail Use% Mounted on (header + lines)
    private static final Pattern DF_LINE = Pattern.compile(
            "^(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(.*)$");

    // free -m: "Mem:   total used free shared buff/cache available"
    private static final Pattern FREE_MEM = Pattern.compile(
            "Mem:\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");
    private static final Pattern FREE_SWAP = Pattern.compile(
            "Swap:\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");

    // uptime: ... load average: 0.00, 0.01, 0.05
    private static final Pattern LOAD_AVG = Pattern.compile(
            "load average:\\s*([\\d.,]+)\\s*,\\s*([\\d.,]+)\\s*,\\s*([\\d.,]+)");

    public LinuxSnapshotService(SshExecutionService sshExecutionService) {
        this.sshExecutionService = sshExecutionService;
    }

    public LinuxSnapshot capture() {
        return capture(null);
    }

    public LinuxSnapshot capture(String serverId) {
        LinuxSnapshot snapshot = new LinuxSnapshot();
        Optional<SshCommandResult> dfResult = serverId != null ? sshExecutionService.executeWithServer(serverId, "df -h") : sshExecutionService.execute("df -h");
        Optional<SshCommandResult> freeResult = serverId != null ? sshExecutionService.executeWithServer(serverId, "free -m") : sshExecutionService.execute("free -m");
        Optional<SshCommandResult> uptimeResult = serverId != null ? sshExecutionService.executeWithServer(serverId, "uptime") : sshExecutionService.execute("uptime");
        Optional<SshCommandResult> cpuResult = serverId != null
                ? sshExecutionService.executeWithServer(serverId, "LC_ALL=C top -bn1 | grep 'Cpu(s)'")
                : sshExecutionService.execute("LC_ALL=C top -bn1 | grep 'Cpu(s)'");

        if (dfResult.isEmpty() && freeResult.isEmpty() && uptimeResult.isEmpty() && cpuResult.isEmpty()) {
            snapshot.setError("SSH not configured or connection failed.");
            return snapshot;
        }

        dfResult.ifPresent(r -> {
            snapshot.setRawDf(r.getStdout());
            if (r.isSuccess()) parseDf(r.getStdout(), snapshot);
        });
        freeResult.ifPresent(r -> {
            snapshot.setRawFree(r.getStdout());
            if (r.isSuccess()) parseFree(r.getStdout(), snapshot);
        });
        uptimeResult.ifPresent(r -> {
            snapshot.setRawUptime(r.getStdout());
            if (r.isSuccess()) parseUptime(r.getStdout(), snapshot);
        });
        cpuResult.ifPresent(r -> {
            snapshot.setRawCpu(r.getStdout());
            if (r.isSuccess()) parseCpu(r.getStdout(), snapshot);
        });

        return snapshot;
    }

    private void parseDf(String output, LinuxSnapshot snapshot) {
        ArrayList<DiskUsage> list = new ArrayList<>();
        try (Scanner sc = new Scanner(output)) {
            if (sc.hasNextLine()) sc.nextLine(); // skip header
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;
                Matcher m = DF_LINE.matcher(line);
                if (m.matches()) {
                    String usePercent = m.group(5);
                    if (usePercent.matches("\\d+%")) {
                        list.add(new DiskUsage(
                                m.group(1), m.group(2), m.group(3), m.group(4),
                                usePercent, m.group(6).trim()));
                    }
                }
            }
        }
        snapshot.setDiskUsage(list);
    }

    private void parseFree(String output, LinuxSnapshot snapshot) {
        MemoryInfo mem = new MemoryInfo();
        Matcher memMatcher = FREE_MEM.matcher(output);
        if (memMatcher.find()) {
            mem.setMemTotalMb(Long.parseLong(memMatcher.group(1)));
            mem.setMemUsedMb(Long.parseLong(memMatcher.group(2)));
            mem.setMemFreeMb(Long.parseLong(memMatcher.group(3)));
            mem.setMemAvailableMb(Long.parseLong(memMatcher.group(6)));
        }
        Matcher swapMatcher = FREE_SWAP.matcher(output);
        if (swapMatcher.find()) {
            mem.setSwapTotalMb(Long.parseLong(swapMatcher.group(1)));
            mem.setSwapUsedMb(Long.parseLong(swapMatcher.group(2)));
            mem.setSwapFreeMb(Long.parseLong(swapMatcher.group(3)));
        }
        snapshot.setMemory(mem);
    }

    private void parseUptime(String output, LinuxSnapshot snapshot) {
        UptimeInfo uptime = new UptimeInfo();
        uptime.setUptimeString(output.trim());
        Matcher m = LOAD_AVG.matcher(output);
        if (m.find()) {
            uptime.setLoad1(m.group(1));
            uptime.setLoad5(m.group(2));
            uptime.setLoad15(m.group(3));
        }
        snapshot.setUptime(uptime);
    }

    private void parseCpu(String output, LinuxSnapshot snapshot) {
        String normalized = output == null ? "" : output.trim();
        if (normalized.isEmpty()) return;

        // Matches top output like:
        // %Cpu(s):  5.6 us,  1.2 sy, ... 92.8 id, ...
        // Cpu(s): 3.0%us, 2.0%sy, ... 95.0%id
        String sanitized = normalized.replace(',', '.').toLowerCase();
        Pattern idlePattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%?\\s*id");
        Matcher idleMatcher = idlePattern.matcher(sanitized);
        if (idleMatcher.find()) {
            try {
                double idle = Double.parseDouble(idleMatcher.group(1));
                double used = Math.max(0.0, Math.min(100.0, 100.0 - idle));
                snapshot.setCpuUsagePercent(used);
                return;
            } catch (NumberFormatException ignored) {
                // continue to fallback parsing below
            }
        }

        Pattern userPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%?\\s*us");
        Pattern systemPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%?\\s*sy");
        Matcher userMatcher = userPattern.matcher(sanitized);
        Matcher systemMatcher = systemPattern.matcher(sanitized);
        if (userMatcher.find() && systemMatcher.find()) {
            try {
                double user = Double.parseDouble(userMatcher.group(1));
                double system = Double.parseDouble(systemMatcher.group(1));
                snapshot.setCpuUsagePercent(Math.max(0.0, Math.min(100.0, user + system)));
            } catch (NumberFormatException ignored) {
                // leave unset when parsing fails
            }
        }
    }
}
