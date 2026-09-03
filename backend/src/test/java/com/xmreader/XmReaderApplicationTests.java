package com.xmreader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xmreader.system.health.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class XmReaderApplicationTests {

    @Autowired
    private HealthService healthService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoadsWithHealthyDatabase() {
        assertThat(healthService.check().components()).containsEntry("database", "UP");
    }

    @Test
    void healthEndpointReturnsUnifiedResponseAndRequestId() throws Exception {
        mockMvc.perform(get("/api/health").header("X-Request-Id", "test-request-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "test-request-123"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.components.database").value("UP"))
                .andExpect(jsonPath("$.requestId").value("test-request-123"));
    }
}
