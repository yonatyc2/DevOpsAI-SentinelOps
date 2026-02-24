package com.sentinelops;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CommandsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void analyze_low_risk_returns_200_and_low() throws Exception {
        mockMvc.perform(post("/api/commands/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"command\":\"df -h\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("LOW"));
    }

    @Test
    void analyze_high_risk_returns_200_and_high() throws Exception {
        mockMvc.perform(post("/api/commands/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"command\":\"rm -rf /\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.reason").isNotEmpty())
                .andExpect(jsonPath("$.rollbackSuggestion").exists());
    }

    @Test
    void execute_low_risk_runs_and_returns_result() throws Exception {
        mockMvc.perform(post("/api/commands/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"command\":\"echo ok\",\"confirmedRiskLevel\":\"LOW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executed").exists());
    }

    @Test
    void history_returns_200_and_array() throws Exception {
        mockMvc.perform(get("/api/commands/history"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
