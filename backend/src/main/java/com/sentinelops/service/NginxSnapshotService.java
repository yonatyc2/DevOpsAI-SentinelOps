package com.sentinelops.service;

import com.sentinelops.model.snapshot.NginxSnapshot;
import com.sentinelops.service.SshExecutionService.SshCommandResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NginxSnapshotService {

    private final SshExecutionService sshExecutionService;

    public NginxSnapshotService(SshExecutionService sshExecutionService) {
        this.sshExecutionService = sshExecutionService;
    }

    public NginxSnapshot capture() {
        return capture(null);
    }

    public NginxSnapshot capture(String serverId) {
        NginxSnapshot snapshot = new NginxSnapshot();

        String statusCmd =
                "if command -v systemctl >/dev/null 2>&1; then " +
                        "systemctl is-active nginx 2>/dev/null || echo unknown; " +
                "elif command -v service >/dev/null 2>&1; then " +
                        "service nginx status >/dev/null 2>&1 && echo active || echo inactive; " +
                "else " +
                        "pgrep -x nginx >/dev/null 2>&1 && echo active || echo inactive; " +
                "fi";

        String localHttpCmd =
                "(curl -s -o /dev/null -w \"%{http_code}\" http://127.0.0.1/ 2>/dev/null " +
                "|| wget -q --server-response -O /dev/null http://127.0.0.1/ 2>&1 | awk '/^  HTTP\\//{print $2}' | tail -n1 " +
                "|| echo 000) | head -n1";

        String logCodeCmd =
                "if [ -r /var/log/nginx/access.log ]; then " +
                    "tail -n 2000 /var/log/nginx/access.log | awk '{c[$9]++} END {for (k in c) print k\"=\"c[k]}'; " +
                "elif [ -r /var/log/nginx/access.log.1 ]; then " +
                    "tail -n 2000 /var/log/nginx/access.log.1 | awk '{c[$9]++} END {for (k in c) print k\"=\"c[k]}'; " +
                "else " +
                    "echo ''; " +
                "fi";
        String ussdLogCmd = buildUssdLogCommand(80);

        Optional<SshCommandResult> statusResult = execute(serverId, statusCmd);
        Optional<SshCommandResult> httpResult = execute(serverId, localHttpCmd);
        Optional<SshCommandResult> logResult = execute(serverId, logCodeCmd);
        Optional<SshCommandResult> ussdLogResult = execute(serverId, ussdLogCmd);

        if (statusResult.isEmpty() && httpResult.isEmpty() && logResult.isEmpty() && ussdLogResult.isEmpty()) {
            snapshot.setError("Nginx check unavailable (SSH not configured or connection failed).");
            return snapshot;
        }

        statusResult.ifPresent(r -> {
            String status = cleanFirstLine(r.getStdout());
            if (status.isEmpty()) status = "unknown";
            snapshot.setServiceStatus(status);
            snapshot.setRunning("active".equalsIgnoreCase(status));
        });

        httpResult.ifPresent(r -> {
            String code = cleanFirstLine(r.getStdout());
            if (!code.isEmpty()) snapshot.setLocalHttpCode(code);
        });

        logResult.ifPresent(r -> snapshot.setResponseCodeCounts(parseCodeCounts(r.getStdout())));
        ussdLogResult.ifPresent(r -> snapshot.setUssdLogLines(parseLogLines(r.getStdout())));

        if (snapshot.getServiceStatus() == null || snapshot.getServiceStatus().isBlank()) {
            snapshot.setServiceStatus("unknown");
        }

        return snapshot;
    }

    private Optional<SshCommandResult> execute(String serverId, String command) {
        return serverId != null && !serverId.isBlank()
                ? sshExecutionService.executeWithServer(serverId, command)
                : sshExecutionService.execute(command);
    }

    public List<String> captureUssdLogLines(String serverId, int limit) {
        int bounded = Math.max(1, Math.min(limit, 500));
        String cmd = buildUssdLogCommand(bounded);
        Optional<SshCommandResult> result = execute(serverId, cmd);
        if (result.isEmpty() || !result.get().isSuccess()) return new ArrayList<>();
        return parseLogLines(result.get().getStdout());
    }

    private String buildUssdLogCommand(int limit) {
        return "if [ -r /var/log/nginx/access.log ]; then " +
                "tail -n 5000 /var/log/nginx/access.log | grep -i ussd | tail -n " + limit + "; " +
                "elif [ -r /var/log/nginx/access.log.1 ]; then " +
                "tail -n 5000 /var/log/nginx/access.log.1 | grep -i ussd | tail -n " + limit + "; " +
                "else " +
                "echo ''; " +
                "fi";
    }

    private String cleanFirstLine(String stdout) {
        if (stdout == null || stdout.isBlank()) return "";
        String[] lines = stdout.split("\n");
        return lines.length == 0 ? "" : lines[0].trim();
    }

    private Map<String, Long> parseCodeCounts(String stdout) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (stdout == null || stdout.isBlank()) return map;
        for (String line : stdout.split("\n")) {
            String item = line.trim();
            if (item.isEmpty()) continue;
            int idx = item.indexOf('=');
            if (idx <= 0 || idx == item.length() - 1) continue;
            String code = item.substring(0, idx).trim();
            String countStr = item.substring(idx + 1).trim();
            if (!code.matches("\\d{3}")) continue;
            try {
                map.put(code, Long.parseLong(countStr));
            } catch (NumberFormatException ignored) {
            }
        }
        return map;
    }

    private List<String> parseLogLines(String stdout) {
        List<String> lines = new ArrayList<>();
        if (stdout == null || stdout.isBlank()) return lines;
        for (String line : stdout.split("\n")) {
            String cleaned = line == null ? "" : line.trim();
            if (!cleaned.isEmpty()) lines.add(cleaned);
        }
        return lines;
    }
}
