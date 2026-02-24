package com.sentinelops.model.snapshot;

/**
 * One container from docker ps / docker stats.
 */
public class ContainerInfo {
    private String id;
    private String name;
    private String image;
    private String state;
    private String status;
    private String uptime;
    private long restartCount;
    private String cpuPercent;
    private String memUsage;
    private String memPercent;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUptime() { return uptime; }
    public void setUptime(String uptime) { this.uptime = uptime; }
    public long getRestartCount() { return restartCount; }
    public void setRestartCount(long restartCount) { this.restartCount = restartCount; }
    public String getCpuPercent() { return cpuPercent; }
    public void setCpuPercent(String cpuPercent) { this.cpuPercent = cpuPercent; }
    public String getMemUsage() { return memUsage; }
    public void setMemUsage(String memUsage) { this.memUsage = memUsage; }
    public String getMemPercent() { return memPercent; }
    public void setMemPercent(String memPercent) { this.memPercent = memPercent; }
}
