package com.sentinelops.model.snapshot;

import java.util.ArrayList;
import java.util.List;

public class DockerSnapshot {
    private List<ContainerInfo> containers = new ArrayList<>();
    private String error;

    public List<ContainerInfo> getContainers() { return containers; }
    public void setContainers(List<ContainerInfo> containers) { this.containers = containers != null ? containers : new ArrayList<>(); }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
