package com.sentinelops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "postgres")
public class PostgresProperties {

    private boolean enabled = false;
    private String host = "localhost";
    private int port = 5432;
    private String user = "postgres";
    private String database = "postgres";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
}
