package com.sentinelops;

import com.sentinelops.service.OpenAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenAiService openAiService;

    @BeforeEach
    void setUp() {
        when(openAiService.chat(anyString(), anyString())).thenReturn("E2E test response from AI");
    }

    @Test
    void chat_returns_200_and_ai_response() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Why is disk full?\",\"includeSystemContext\":false}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.response").value("E2E test response from AI"));
    }

    @Test
    void chat_with_system_context_calls_openai_with_context() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Summarize system state\",\"includeSystemContext\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("E2E test response from AI"));
    }

    @Test
    void chat_empty_message_returns_400() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\",\"includeSystemContext\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.response").value("Message cannot be empty."));
    }

    @Test
    void chat_blank_message_returns_400() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \",\"includeSystemContext\":false}"))
                .andExpect(status().isBadRequest());
    }
}
