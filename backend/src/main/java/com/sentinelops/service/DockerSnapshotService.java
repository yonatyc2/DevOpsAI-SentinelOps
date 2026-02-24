package com.sentinelops.service;

import com.sentinelops.model.snapshot.ContainerInfo;
import com.sentinelops.model.snapshot.DockerSnapshot;
import com.sentinelops.service.SshExecutionService.SshCommandResult;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Collects Docker data via SSH: docker ps, docker stats, restart counts from inspect.
 */
@Service
public class DockerSnapshotService {

    private final SshExecutionService sshExecutionService;

    public DockerSnapshotService(SshExecutionService sshExecutionService) {
        this.sshExecutionService = sshExecutionService;
    }

    public DockerSnapshot capture() {
        return capture(null);
    }

    public DockerSnapshot capture(String serverId) {
        DockerSnapshot snapshot = new DockerSnapshot();
        String cmd = "docker ps -a --format \"{{.ID}}\t{{.Names}}\t{{.Image}}\t{{.State}}\t{{.Status}}\" 2>/dev/null";
        Optional<SshCommandResult> psResult = serverId != null ? sshExecutionService.executeWithServer(serverId, cmd) : sshExecutionService.execute(cmd);
        if (psResult.isEmpty() || !psResult.get().isSuccess()) {
            snapshot.setError("Docker not available or SSH failed.");
            return snapshot;
        }
        String psOut = psResult.get().getStdout();
        if (psOut.isBlank()) {
            return snapshot;
        }

        List<ContainerInfo> containers = parseDockerPs(psOut);
        mergeStats(containers, serverId);
        mergeRestartCounts(containers, serverId);
        snapshot.setContainers(containers);
        return snapshot;
    }

    private List<ContainerInfo> parseDockerPs(String output) {
        List<ContainerInfo> list = new ArrayList<>();
        for (String line : output.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\t", -1);
            if (parts.length >= 5) {
                ContainerInfo c = new ContainerInfo();
                c.setId(parts[0].trim());
                c.setName(parts[1].trim());
                c.setImage(parts[2].trim());
                c.setState(parts[3].trim());
                c.setStatus(parts[4].trim());
                c.setUptime(extractUptime(parts[4].trim()));
                list.add(c);
            }
        }
        return list;
    }

    private String extractUptime(String status) {
        if (status == null) return "";
        String trimmed = status.trim();
        if (trimmed.isEmpty()) return "";
        if (!trimmed.toLowerCase().startsWith("up ")) return "";
        int openParen = trimmed.indexOf('(');
        if (openParen > 0) {
            return trimmed.substring(0, openParen).trim();
        }
        return trimmed;
    }

    private void mergeStats(List<ContainerInfo> containers, String serverId) {
        String cmd = "docker stats --no-stream --format \"{{.ID}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}\" 2>/dev/null";
        Optional<SshCommandResult> result = serverId != null ? sshExecutionService.executeWithServer(serverId, cmd) : sshExecutionService.execute(cmd);
        if (result.isEmpty() || !result.get().isSuccess()) return;
        String out = result.get().getStdout();
        Map<String, String[]> byId = new HashMap<>();
        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\t", -1);
            if (parts.length >= 4) {
                String id = parts[0].length() >= 12 ? parts[0].substring(0, 12) : parts[0];
                byId.put(id, new String[]{ parts[1], parts[2], parts[3] });
            }
        }
        for (ContainerInfo c : containers) {
            String shortId = c.getId().length() >= 12 ? c.getId().substring(0, 12) : c.getId();
            String[] stats = byId.get(shortId);
            if (stats != null) {
                c.setCpuPercent(stats[0]);
                c.setMemUsage(stats[1]);
                c.setMemPercent(stats[2]);
            }
        }
    }

    private void mergeRestartCounts(List<ContainerInfo> containers, String serverId) {
        if (containers.isEmpty()) return;
        StringBuilder ids = new StringBuilder();
        for (ContainerInfo c : containers) {
            if (ids.length() > 0) ids.append(" ");
            ids.append(c.getId());
        }
        String cmd = "docker inspect " + ids + " --format '{{.Id}} {{.RestartCount}}' 2>/dev/null";
        Optional<SshCommandResult> result = serverId != null ? sshExecutionService.executeWithServer(serverId, cmd) : sshExecutionService.execute(cmd);
        if (result.isEmpty() || !result.get().isSuccess()) return;
        String out = result.get().getStdout();
        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int lastSpace = line.lastIndexOf(' ');
            if (lastSpace <= 0) continue;
            String fullId = line.substring(0, lastSpace).trim();
            String countStr = line.substring(lastSpace + 1).trim();
            long count = 0;
            try {
                count = Long.parseLong(countStr);
            } catch (NumberFormatException ignored) {}
            for (ContainerInfo c : containers) {
                if (fullId.startsWith(c.getId()) || c.getId().startsWith(fullId.length() >= 12 ? fullId.substring(0, 12) : fullId)) {
                    c.setRestartCount(count);
                    break;
                }
            }
        }
    }
}
