package com.sentinelops.model.snapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL snapshot: activity count, database sizes, slow queries, locks.
 */
public class PostgresSnapshot {
    private int activeConnections;
    private List<DatabaseSize> databaseSizes = new ArrayList<>();
    private String slowQueriesSummary;
    private String locksSummary;
    private String error;

    public int getActiveConnections() { return activeConnections; }
    public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }
    public List<DatabaseSize> getDatabaseSizes() { return databaseSizes; }
    public void setDatabaseSizes(List<DatabaseSize> databaseSizes) { this.databaseSizes = databaseSizes != null ? databaseSizes : new ArrayList<>(); }
    public String getSlowQueriesSummary() { return slowQueriesSummary; }
    public void setSlowQueriesSummary(String slowQueriesSummary) { this.slowQueriesSummary = slowQueriesSummary; }
    public String getLocksSummary() { return locksSummary; }
    public void setLocksSummary(String locksSummary) { this.locksSummary = locksSummary; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public static class DatabaseSize {
        private String name;
        private String size;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
    }
}
