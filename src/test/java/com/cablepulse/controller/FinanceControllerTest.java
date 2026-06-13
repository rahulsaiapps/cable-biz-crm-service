package com.cablepulse.controller;

import com.cablepulse.repository.EmployeeRepository;
import com.cablepulse.repository.WorkspaceRepository;
import com.cablepulse.testsupport.TestWorkspaceSupport;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FinanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private TestWorkspaceSupport workspaceSupport;

    @MockBean
    private FirebaseAuth firebaseAuth;

    private String e2eId;
    private String sessionId;

    @BeforeEach
    void setUp() throws Exception {
        e2eId = UUID.randomUUID().toString();
        sessionId = UUID.randomUUID().toString();

        workspaceSupport.seedDefaultWorkspace();
        employeeRepository.save(workspaceSupport.ownerEmployee());

        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn("owner-uid");
        when(firebaseToken.getClaims()).thenReturn(Map.of("role", "OWNER"));
        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(firebaseToken);
    }

    @Test
    void getDailyLedger_returnsBookEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/finance/daily-ledger")
                        .param("targetDate", "2026-06-10")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary").exists())
                .andExpect(jsonPath("$.data.transactions").isArray());
    }

    @Test
    void getFinanceMetrics_returnsKpis() throws Exception {
        mockMvc.perform(get("/api/v1/finance/metrics")
                        .param("interval", "6M")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.net_profit").exists());
    }

    @Test
    void getAlertTargetSize_returnsCount() throws Exception {
        mockMvc.perform(get("/api/v1/alerts/target-size")
                        .param("region", "Kolamuru Village")
                        .param("block", "School Road")
                        .param("customer_types", "CABLE,INTERNET")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.target_size").exists());
    }
}
