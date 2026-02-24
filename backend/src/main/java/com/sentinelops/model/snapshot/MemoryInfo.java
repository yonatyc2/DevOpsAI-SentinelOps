package com.sentinelops.model.snapshot;

/**
 * Parsed from free -m (Mem and Swap).
 */
public class MemoryInfo {
    private long memTotalMb;
    private long memUsedMb;
    private long memFreeMb;
    private long memAvailableMb;
    private long swapTotalMb;
    private long swapUsedMb;
    private long swapFreeMb;

    public MemoryInfo() {}

    public long getMemTotalMb() { return memTotalMb; }
    public void setMemTotalMb(long memTotalMb) { this.memTotalMb = memTotalMb; }
    public long getMemUsedMb() { return memUsedMb; }
    public void setMemUsedMb(long memUsedMb) { this.memUsedMb = memUsedMb; }
    public long getMemFreeMb() { return memFreeMb; }
    public void setMemFreeMb(long memFreeMb) { this.memFreeMb = memFreeMb; }
    public long getMemAvailableMb() { return memAvailableMb; }
    public void setMemAvailableMb(long memAvailableMb) { this.memAvailableMb = memAvailableMb; }
    public long getSwapTotalMb() { return swapTotalMb; }
    public void setSwapTotalMb(long swapTotalMb) { this.swapTotalMb = swapTotalMb; }
    public long getSwapUsedMb() { return swapUsedMb; }
    public void setSwapUsedMb(long swapUsedMb) { this.swapUsedMb = swapUsedMb; }
    public long getSwapFreeMb() { return swapFreeMb; }
    public void setSwapFreeMb(long swapFreeMb) { this.swapFreeMb = swapFreeMb; }
}
