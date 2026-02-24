package com.sentinelops.model.snapshot;

/**
 * Parsed from uptime (load averages).
 */
public class UptimeInfo {
    private String uptimeString;
    private String load1;
    private String load5;
    private String load15;

    public UptimeInfo() {}

    public UptimeInfo(String uptimeString, String load1, String load5, String load15) {
        this.uptimeString = uptimeString;
        this.load1 = load1;
        this.load5 = load5;
        this.load15 = load15;
    }

    public String getUptimeString() { return uptimeString; }
    public void setUptimeString(String uptimeString) { this.uptimeString = uptimeString; }
    public String getLoad1() { return load1; }
    public void setLoad1(String load1) { this.load1 = load1; }
    public String getLoad5() { return load5; }
    public void setLoad5(String load5) { this.load5 = load5; }
    public String getLoad15() { return load15; }
    public void setLoad15(String load15) { this.load15 = load15; }
}
