package com.sentinelops.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelops.model.snapshot.SystemSnapshot;
import org.springframework.stereotype.Service;

/**
 * Orchestrates chat: builds system prompt with optional structured snapshot, calls OpenAI.
 */
@Service
public class ChatService {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
        You are SentinelOps AI, an expert DevOps/SRE assistant for Linux, Docker, and PostgreSQL.
        You help engineers understand infrastructure issues, diagnose problems, and suggest safe remediation steps.

        Guidelines:
        - Answer in clear, concise plain English.
        - When discussing disk, LVM, Docker, or Postgres, be accurate and cautious.
        - Never suggest destructive commands (e.g. rm -rf, lvreduce, drop database) without explicit warning.
        - If the user asks "can I safely...", always explain risks and suggest verification steps first.
        - When structured system snapshot (JSON) is provided below, use it to inform your answer. Identify issues like full disks, high restart counts, memory pressure, or Postgres locks.

        Structured system snapshot:
        %s
        """;

    private final OpenAiService openAiService;
    private final SnapshotAggregatorService snapshotAggregatorService;
    private final ObjectMapper objectMapper;

    public ChatService(OpenAiService openAiService, SnapshotAggregatorService snapshotAggregatorService, ObjectMapper objectMapper) {
        this.openAiService = openAiService;
        this.snapshotAggregatorService = snapshotAggregatorService;
        this.objectMapper = objectMapper;
    }

    public String chat(String userMessage) {
        return chat(userMessage, false, null);
    }

    public String mode() {
        return openAiService.isConfigured() ? "OPENAI" : "LOCAL";
    }

    /**
     * @param includeSystemContext if true, capture full system snapshot (Linux + Docker + Postgres) and send as JSON to AI
     * @param serverId optional server to run snapshot against; null uses default SSH config
     */
    public String chat(String userMessage, boolean includeSystemContext, String serverId) {
        String systemContext = "No live system context was gathered. Answer based on general DevOps knowledge.";
        if (includeSystemContext) {
            SystemSnapshot snapshot = snapshotAggregatorService.capture(serverId);
            try {
                systemContext = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot);
            } catch (JsonProcessingException e) {
                systemContext = "Snapshot serialization failed: " + e.getMessage();
            }
        }
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, systemContext);
        return openAiService.chat(userMessage, systemPrompt);
    }
}
