package com.sentinelops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "encryption")
public class EncryptionProperties {

    /** Base64 or raw secret used to derive AES key (min 16 chars recommended). */
    private String secret = "sentinelops-default-change-in-production";

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}
