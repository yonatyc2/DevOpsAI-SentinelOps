package com.sentinelops;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SnapshotControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getSnapshot_returns_200_and_json_structure() throws Exception {
        mockMvc.perform(get("/api/snapshot"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.linux").exists())
                .andExpect(jsonPath("$.docker").exists())
                .andExpect(jsonPath("$.postgres").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
