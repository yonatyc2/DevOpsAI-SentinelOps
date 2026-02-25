package com.sentinelops.model.snapshot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class NginxSnapshot {
    private String serviceStatus;
    private boolean running;
    private String localHttpCode;
    private Map<String, Long> responseCodeCounts = new LinkedHashMap<>();
    private List<String> ussdLogLines = new ArrayList<>();
    private String error;

    public String getServiceStatus() { return serviceStatus; }
    public void setServiceStatus(String serviceStatus) { this.serviceStatus = serviceStatus; }
    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }
    public String getLocalHttpCode() { return localHttpCode; }
    public void setLocalHttpCode(String localHttpCode) { this.localHttpCode = localHttpCode; }
    public Map<String, Long> getResponseCodeCounts() { return responseCodeCounts; }
    public void setResponseCodeCounts(Map<String, Long> responseCodeCounts) {
        this.responseCodeCounts = responseCodeCounts != null ? responseCodeCounts : new LinkedHashMap<>();
    }
    public List<String> getUssdLogLines() { return ussdLogLines; }
    public void setUssdLogLines(List<String> ussdLogLines) {
        this.ussdLogLines = ussdLogLines != null ? ussdLogLines : new ArrayList<>();
    }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
