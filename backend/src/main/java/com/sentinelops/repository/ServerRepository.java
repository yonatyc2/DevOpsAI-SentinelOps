package com.sentinelops.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelops.model.Server;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory server list with file persistence (data/servers.json).
 */
@Repository
public class ServerRepository {

    private static final Path DATA_FILE = Paths.get("data", "servers.json");

    private final List<Server> servers = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    void load() {
        try {
            if (Files.exists(DATA_FILE)) {
                String json = Files.readString(DATA_FILE);
                List<Server> loaded = objectMapper.readValue(json, new TypeReference<>() {});
                servers.clear();
                if (loaded != null) servers.addAll(loaded);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private void save() {
        try {
            Files.createDirectories(DATA_FILE.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(DATA_FILE.toFile(), servers);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save servers", e);
        }
    }

    public List<Server> findAll() {
        return new ArrayList<>(servers);
    }

    public Optional<Server> findById(String id) {
        return servers.stream().filter(s -> id.equals(s.getId())).findFirst();
    }

    public Server save(Server server) {
        if (server.getId() == null || server.getId().isBlank()) {
            server.setId(UUID.randomUUID().toString());
            servers.add(server);
        } else {
            servers.removeIf(s -> s.getId().equals(server.getId()));
            servers.add(server);
        }
        save();
        return server;
    }

    public boolean deleteById(String id) {
        boolean removed = servers.removeIf(s -> s.getId().equals(id));
        if (removed) save();
        return removed;
    }
}
