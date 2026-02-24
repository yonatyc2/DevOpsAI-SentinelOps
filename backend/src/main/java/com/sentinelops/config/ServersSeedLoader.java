package com.sentinelops.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelops.model.Server;
import com.sentinelops.repository.ServerRepository;
import com.sentinelops.service.CredentialEncryptionService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

/**
 * On startup, if data/servers-seed.json exists, loads servers from it (plain credentials),
 * encrypts and saves them, then renames the file so it is not loaded again.
 */
@Component
@Order(100)
public class ServersSeedLoader implements ApplicationRunner {

    private static final Path SEED_FILE = Paths.get("data", "servers-seed.json");
    private static final Path SEED_DONE = Paths.get("data", "servers-seed.json.done");

    private final ServerRepository serverRepository;
    private final CredentialEncryptionService encryptionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ServersSeedLoader(ServerRepository serverRepository, CredentialEncryptionService encryptionService) {
        this.serverRepository = serverRepository;
        this.encryptionService = encryptionService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!Files.exists(SEED_FILE)) return;
        try {
            String json = Files.readString(SEED_FILE);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = objectMapper.readValue(json, List.class);
            List<Server> existing = serverRepository.findAll();
            for (Map<String, Object> e : entries) {
                String host = string(e, "host");
                int port = e.get("port") != null ? ((Number) e.get("port")).intValue() : 22;
                if (host == null || host.isBlank()) continue;
                boolean already = existing.stream().anyMatch(s -> host.equals(s.getHost()) && port == s.getPort());
                if (already) continue;
                Server server = new Server();
                server.setName(string(e, "name") != null ? string(e, "name") : host);
                server.setHost(host);
                server.setPort(port);
                server.setUsername(string(e, "username"));
                server.setAuthType(Server.AuthType.PASSWORD);
                String password = string(e, "password");
                if (password != null && !password.isBlank())
                    server.setEncryptedCredential(encryptionService.encrypt(password));
                server.setHealth("unknown");
                serverRepository.save(server);
            }
            Files.move(SEED_FILE, SEED_DONE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            // log but do not fail startup
        }
    }

    private static String string(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }
}
