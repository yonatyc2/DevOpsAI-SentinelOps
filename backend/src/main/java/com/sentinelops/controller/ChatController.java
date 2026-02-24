package com.sentinelops.controller;

import com.sentinelops.service.ChatService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("response", "Message cannot be empty.", "mode", chatService.mode()));
        }
        boolean withContext = request.isIncludeSystemContext();
        String serverId = request.getServerId();
        String response = chatService.chat(request.getMessage().trim(), withContext, serverId);
        return ResponseEntity.ok(Map.of("response", response, "mode", chatService.mode()));
    }

    @GetMapping("/mode")
    public ResponseEntity<Map<String, String>> mode() {
        String mode = chatService.mode();
        return ResponseEntity.ok(Map.of("mode", mode));
    }

    public static class ChatRequest {
        @NotBlank
        private String message;
        private boolean includeSystemContext;
        private String serverId;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public boolean isIncludeSystemContext() { return includeSystemContext; }
        public void setIncludeSystemContext(boolean includeSystemContext) { this.includeSystemContext = includeSystemContext; }
        public String getServerId() { return serverId; }
        public void setServerId(String serverId) { this.serverId = serverId; }
    }
}
