package com.sentinelops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ssh")
public class SshProperties {

    private String host = "localhost";
    private int port = 22;
    private String username = "root";
    private String privateKeyPath;
    private String password;
    private int connectTimeoutMs = 10_000;
    private int commandTimeoutMs = 30_000;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPrivateKeyPath() { return privateKeyPath; }
    public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getCommandTimeoutMs() { return commandTimeoutMs; }
    public void setCommandTimeoutMs(int commandTimeoutMs) { this.commandTimeoutMs = commandTimeoutMs; }
}
