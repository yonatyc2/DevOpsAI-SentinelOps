package com.sentinelops.service;

import com.sentinelops.config.OpenAiProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Integrates with OpenAI Chat Completions API for AI responses.
 */
@Service
public class OpenAiService {

    private final OpenAiProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Pattern GREETING_PATTERN = Pattern.compile("^(hi|hello|hey|yo|howdy)\\b.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMAND_LIKE_PATTERN = Pattern.compile(
            "^(df|du|free|top|uptime|ps|systemctl|journalctl|docker|kubectl|netstat|ss|iostat|vmstat|lsblk|cat|grep|tail|head)\\b.*",
            Pattern.CASE_INSENSITIVE);

    public OpenAiService(OpenAiProperties properties) {
        this.properties = properties;
    }

    public String chat(String userMessage, String systemContext) {
        String apiKey = properties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return localFallbackResponse(userMessage);
        }

        String url = properties.getBaseUrl().replaceAll("/$", "") + "/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        List<Map<String, String>> messages = new java.util.ArrayList<>();
        if (systemContext != null && !systemContext.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemContext));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "messages", messages,
                "max_tokens", 1024
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<OpenAiResponse> response = restTemplate.postForEntity(url, request, OpenAiResponse.class);
            if (response.getBody() == null || response.getBody().getChoices() == null || response.getBody().getChoices().isEmpty()) {
                return "No response from OpenAI.";
            }
            Choice choice = response.getBody().getChoices().get(0);
            String content = choice.getMessage() != null ? choice.getMessage().getContent() : null;
            return content != null ? content : "";
        } catch (HttpStatusCodeException e) {
            String msg = e.getStatusCode() + (e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isBlank()
                    ? ": " + e.getResponseBodyAsString().substring(0, Math.min(200, e.getResponseBodyAsString().length()))
                    : "");
            return "OpenAI request failed: " + msg + ". Check your API key and quota.";
        } catch (Exception e) {
            return "OpenAI request failed: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    public boolean isConfigured() {
        String apiKey = properties.getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    private String localFallbackResponse(String userMessage) {
        String question = userMessage == null ? "" : userMessage.trim();
        if (question.isEmpty()) {
            return """
                    Local mode is active (OpenAI key not configured).
                    Ask me about Linux, Docker, PostgreSQL, or paste a command and I will explain it.
                    """;
        }

        if (GREETING_PATTERN.matcher(question).matches()) {
            return """
                    Local mode is active (OpenAI key not configured), but I can still help.

                    Try one of these:
                    - "Explain this command: df -h"
                    - "What does high CPU usually mean?"
                    - "How do I debug Docker restart loops?"
                    """;
        }

        if (COMMAND_LIKE_PATTERN.matcher(question).matches()) {
            return """
                    Local mode is active, so I cannot run AI reasoning yet, but here is a direct explanation:

                    `%s`

                    - This looks like a shell command.
                    - To run it on a selected server in SentinelOps, use **Execute command**.
                    - For `df -h`, focus on mounts above 75%% (warning) and 90%% (critical), and compare Used/Total GB.

                    If you want full conversational AI answers, set `OPENAI_API_KEY` and restart backend.
                    """.formatted(question);
        }

        return """
                SentinelOps local mode is active because OpenAI is not configured.

                I can still provide practical DevOps guidance for your question:
                "%s"

                Suggested next checks:
                - If disk related: run `df -h` and inspect high-use mounts.
                - If CPU related: run `top -bn1 | grep 'Cpu(s)'` and identify top processes with `ps aux --sort=-%%cpu | head`.
                - If Docker related: run `docker ps -a` and inspect restarts/status.
                - If DB related: check active connections and long-running queries.

                For richer, contextual AI responses, set `OPENAI_API_KEY` and restart backend.
                """.formatted(question);
    }

    public static class OpenAiResponse {
        private List<Choice> choices;
        public List<Choice> getChoices() { return choices; }
        public void setChoices(List<Choice> choices) { this.choices = choices; }
    }

    public static class Choice {
        private Message message;
        public Message getMessage() { return message; }
        public void setMessage(Message message) { this.message = message; }
    }

    public static class Message {
        private String content;
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
