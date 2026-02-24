package com.sentinelops.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * SSH server definition. Credential stored encrypted; never expose decrypted value in API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Server {
    private String id;
    private String name;
    private String host;
    private int port = 22;
    private String username;
    private AuthType authType;
    private String encryptedCredential;  // encrypted password or private key content
    private String health;               // last health check: OK, FAIL, unknown

    public enum AuthType {
        PASSWORD,
        PRIVATE_KEY
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public AuthType getAuthType() { return authType; }
    public void setAuthType(AuthType authType) { this.authType = authType; }
    public String getEncryptedCredential() { return encryptedCredential; }
    public void setEncryptedCredential(String encryptedCredential) { this.encryptedCredential = encryptedCredential; }
    public String getHealth() { return health; }
    public void setHealth(String health) { this.health = health; }
}
