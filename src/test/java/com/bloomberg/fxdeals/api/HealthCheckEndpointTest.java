package com.bloomberg.fxdeals.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for health check endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application.properties")
class HealthCheckEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testLivenessProbe() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testReadinessProbe() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testHealthDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").exists())
                .andExpect(jsonPath("$.components.db").exists())
                .andExpect(jsonPath("$.components.diskSpace").exists())
                .andExpect(jsonPath("$.components.fxDeals").exists());
    }

    @Test
    void testInfoEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").exists())
                .andExpect(jsonPath("$.application.name").value("FX Deals Data Warehouse"))
                .andExpect(jsonPath("$.capabilities").exists());
    }

    @Test
    void testCustomHealthIndicator() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.fxDeals.status").value("UP"))
                .andExpect(jsonPath("$.components.fxDeals.details.service")
                        .value("FX Deals Data Warehouse"))
                .andExpect(jsonPath("$.components.fxDeals.details.database")
                        .value("Connected"))
                .andExpect(jsonPath("$.components.fxDeals.details.totalDeals").exists());
    }
}

