package com.cablepulse.controller;

import com.cablepulse.model.Customer;
import com.cablepulse.model.GlobalPlan;
import com.cablepulse.model.Territory;
import com.cablepulse.repository.CustomerLedgerRepository;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.DailyTransactionRepository;
import com.cablepulse.repository.GlobalPlanRepository;
import com.cablepulse.repository.TerritoryRepository;
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

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TerritoryRepository territoryRepository;

    @Autowired
    private GlobalPlanRepository globalPlanRepository;

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
    private String customerId;
    private String territoryId;

    @BeforeEach
    void setUp() throws Exception {
        e2eId = UUID.randomUUID().toString();
        sessionId = UUID.randomUUID().toString();
        testDatabaseCleaner.wipeCoreWorkspaceData();

        territoryId = "ter_" + UUID.randomUUID().toString().replace("-", "");
        territoryRepository.save(new Territory(territoryId, "Kolamuru"));

        customerId = "cust_" + UUID.randomUUID().toString().replace("-", "");
        GlobalPlan plan = globalPlanRepository.save(
                new GlobalPlan("plan-basic", "Basic Pack", new BigDecimal("199.00"), "SD"));
        Customer customer = new Customer(
                customerId,
                1,
                "Satish Kumar",
                "9876543210",
                "School Road",
                "4-12/A",
                new BigDecimal("199.00"),
                territoryRepository.findById(territoryId).orElseThrow(),
                plan
        );
        customerRepository.save(customer);

        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn("owner-uid");
        when(firebaseToken.getClaims()).thenReturn(Map.of("role", "OWNER"));
        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(firebaseToken);
    }

    @Test
    void getCustomerProfile_returnsProfile() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}", customerId)
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerId").value(customerId))
                .andExpect(jsonPath("$.data.fullName").value("Satish Kumar"))
                .andExpect(jsonPath("$.data.activePlanName").value("Basic Pack"));
    }

    @Test
    void collectPayment_createsLedgerAndTransaction() throws Exception {
        mockMvc.perform(post("/api/v1/customers/{id}/payments", customerId)
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 500,
                                  "monthsPaid": ["JAN", "FEB"],
                                  "year": 2026
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void updateSubscription_returnsUpdatedProfile() throws Exception {
        mockMvc.perform(put("/api/v1/customers/{id}/subscription", customerId)
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "plan_name": "Premium HD",
                                  "plan_monthly_rate": 350
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activePlanName").value("Premium HD"))
                .andExpect(jsonPath("$.data.monthlyRate").value(350));
    }
}
