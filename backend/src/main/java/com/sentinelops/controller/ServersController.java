package com.sentinelops.controller;

import com.sentinelops.model.Server;
import com.sentinelops.repository.ServerRepository;
import com.sentinelops.service.CredentialEncryptionService;
import com.sentinelops.service.SshExecutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/servers")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*")
public class ServersController {

    private final ServerRepository serverRepository;
    private final CredentialEncryptionService encryptionService;
    private final SshExecutionService sshExecutionService;

    public ServersController(ServerRepository serverRepository, CredentialEncryptionService encryptionService,
                             SshExecutionService sshExecutionService) {
        this.serverRepository = serverRepository;
        this.encryptionService = encryptionService;
        this.sshExecutionService = sshExecutionService;
    }

    @GetMapping
    public List<Server> list() {
        return serverRepository.findAll().stream()
                .map(this::sanitizeForApi)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Server> get(@PathVariable String id) {
        Optional<Server> s = serverRepository.findById(id);
        if (s.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(sanitizeForApi(s.get()));
    }

    @GetMapping("/{id}/health")
    public ResponseEntity<Map<String, String>> health(@PathVariable String id) {
        Optional<Server> s = serverRepository.findById(id);
        if (s.isEmpty()) return ResponseEntity.notFound().build();
        var result = sshExecutionService.executeWithServer(id, "echo ok");
        String status = result.map(r -> r.isSuccess() ? "OK" : "FAIL").orElse("FAIL");
        if (result.isPresent() && result.get().isSuccess()) {
            serverRepository.findById(id).ifPresent(server -> {
                server.setHealth("OK");
                serverRepository.save(server);
            });
        } else {
            serverRepository.findById(id).ifPresent(server -> {
                server.setHealth("FAIL");
                serverRepository.save(server);
            });
        }
        return ResponseEntity.ok(Map.of("health", status));
    }

    @PostMapping
    public ResponseEntity<Server> create(@RequestBody ServerRequest request) {
        Server server = new Server();
        server.setName(request.getName());
        server.setHost(request.getHost());
        server.setPort(request.getPort() > 0 ? request.getPort() : 22);
        server.setUsername(request.getUsername());
        server.setAuthType(request.getAuthType() != null ? request.getAuthType() : Server.AuthType.PASSWORD);
        String plain = request.getPassword() != null ? request.getPassword() : request.getPrivateKey();
        if (plain == null || plain.isBlank())
            return ResponseEntity.badRequest().build();
        server.setEncryptedCredential(encryptionService.encrypt(plain));
        server.setHealth("unknown");
        Server saved = serverRepository.save(server);
        return ResponseEntity.ok(sanitizeForApi(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Server> update(@PathVariable String id, @RequestBody ServerRequest request) {
        Optional<Server> existing = serverRepository.findById(id);
        if (existing.isEmpty()) return ResponseEntity.notFound().build();
        Server server = existing.get();
        if (request.getName() != null) server.setName(request.getName());
        if (request.getHost() != null) server.setHost(request.getHost());
        if (request.getPort() > 0) server.setPort(request.getPort());
        if (request.getUsername() != null) server.setUsername(request.getUsername());
        if (request.getAuthType() != null) server.setAuthType(request.getAuthType());
        if (request.getPassword() != null && !request.getPassword().isBlank())
            server.setEncryptedCredential(encryptionService.encrypt(request.getPassword()));
        else if (request.getPrivateKey() != null && !request.getPrivateKey().isBlank())
            server.setEncryptedCredential(encryptionService.encrypt(request.getPrivateKey()));
        Server saved = serverRepository.save(server);
        return ResponseEntity.ok(sanitizeForApi(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return serverRepository.deleteById(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    public static class ServerRequest {
        private String name;
        private String host;
        private int port = 22;
        private String username;
        private Server.AuthType authType;
        private String password;
        private String privateKey;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public Server.AuthType getAuthType() { return authType; }
        public void setAuthType(Server.AuthType authType) { this.authType = authType; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getPrivateKey() { return privateKey; }
        public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    }

    private Server sanitizeForApi(Server source) {
        Server safe = new Server();
        safe.setId(source.getId());
        safe.setName(source.getName());
        safe.setHost(source.getHost());
        safe.setPort(source.getPort());
        safe.setUsername(source.getUsername());
        safe.setAuthType(source.getAuthType());
        safe.setHealth(source.getHealth());
        safe.setEncryptedCredential(null);
        return safe;
    }
}
