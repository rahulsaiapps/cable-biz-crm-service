package com.cablepulse.controller;

import com.cablepulse.model.ConnectionProvider;
import com.cablepulse.repository.ConnectionProviderRepository;
import com.cablepulse.repository.CustomerLedgerRepository;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.DailyTransactionRepository;
import com.cablepulse.repository.GlobalPlanRepository;
import com.cablepulse.testsupport.TestDatabaseCleaner;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PlanControllerCreateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConnectionProviderRepository connectionProviderRepository;

    @Autowired
    private GlobalPlanRepository globalPlanRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerLedgerRepository customerLedgerRepository;

    @Autowired
    private DailyTransactionRepository dailyTransactionRepository;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @MockBean
    private FirebaseAuth firebaseAuth;

    private String e2eId;
    private String sessionId;

    @BeforeEach
    void setUp() throws Exception {
        e2eId = UUID.randomUUID().toString();
        sessionId = UUID.randomUUID().toString();
        testDatabaseCleaner.wipeCoreWorkspaceData();
        connectionProviderRepository.save(new ConnectionProvider("Skynet Cable Networks"));

        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn("owner-uid");
        when(firebaseToken.getClaims()).thenReturn(Map.of("role", "OWNER"));
        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(firebaseToken);
    }

    @Test
    void createPlan_acceptsOptionalChannelsText() throws Exception {
        mockMvc.perform(post("/api/v1/plans")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Gold HD Pack",
                                  "price": 350,
                                  "channels_text": "120+ Channels, HD Support",
                                  "is_hd": true,
                                  "provider": "Skynet Cable Networks"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.createdPlanId").exists());

        var plans = globalPlanRepository.findByProvider_Name("Skynet Cable Networks");
        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).getChannelsText()).isEqualTo("120+ Channels, HD Support");
        assertThat(plans.get(0).getHd()).isTrue();
    }

    @Test
    void createPlan_allowsMissingOptionalFields() throws Exception {
        mockMvc.perform(post("/api/v1/plans")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Silver SD Pack",
                                  "price": 220,
                                  "provider": "Skynet Cable Networks"
                                }
                                """))
                .andExpect(status().isCreated());

        var plans = globalPlanRepository.findByProvider_Name("Skynet Cable Networks");
        assertThat(plans).hasSize(1);
        assertThat(plans.get(0).getChannelsText()).isNull();
        assertThat(plans.get(0).getHd()).isFalse();
    }
}
