package com.sentinelops.model.snapshot;

import java.util.ArrayList;
import java.util.List;

public class LinuxSnapshot {
    private List<DiskUsage> diskUsage = new ArrayList<>();
    private MemoryInfo memory = new MemoryInfo();
    private UptimeInfo uptime = new UptimeInfo();
    private Double cpuUsagePercent;
    private String rawDf;
    private String rawFree;
    private String rawUptime;
    private String rawCpu;
    private String error;

    public List<DiskUsage> getDiskUsage() { return diskUsage; }
    public void setDiskUsage(List<DiskUsage> diskUsage) { this.diskUsage = diskUsage != null ? diskUsage : new ArrayList<>(); }
    public MemoryInfo getMemory() { return memory; }
    public void setMemory(MemoryInfo memory) { this.memory = memory != null ? memory : new MemoryInfo(); }
    public UptimeInfo getUptime() { return uptime; }
    public void setUptime(UptimeInfo uptime) { this.uptime = uptime != null ? uptime : new UptimeInfo(); }
    public Double getCpuUsagePercent() { return cpuUsagePercent; }
    public void setCpuUsagePercent(Double cpuUsagePercent) { this.cpuUsagePercent = cpuUsagePercent; }
    public String getRawDf() { return rawDf; }
    public void setRawDf(String rawDf) { this.rawDf = rawDf; }
    public String getRawFree() { return rawFree; }
    public void setRawFree(String rawFree) { this.rawFree = rawFree; }
    public String getRawUptime() { return rawUptime; }
    public void setRawUptime(String rawUptime) { this.rawUptime = rawUptime; }
    public String getRawCpu() { return rawCpu; }
    public void setRawCpu(String rawCpu) { this.rawCpu = rawCpu; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
