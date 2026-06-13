package com.cablepulse.controller;

import com.cablepulse.model.Customer;
import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.model.GlobalPlan;
import com.cablepulse.model.Territory;
import com.cablepulse.model.Workspace;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.EmployeeRepository;
import com.cablepulse.repository.GlobalPlanRepository;
import com.cablepulse.repository.TerritoryRepository;
import com.cablepulse.repository.WorkspaceRepository;
import com.cablepulse.security.JwtTokenService;
import com.cablepulse.testsupport.TestDatabaseCleaner;
import com.cablepulse.testsupport.TestWorkspaceSupport;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkspaceIsolationControllerTest {

    private static final String OTHER_WORKSPACE_ID = "ws_other";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TerritoryRepository territoryRepository;

    @Autowired
    private GlobalPlanRepository globalPlanRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @Autowired
    private TestWorkspaceSupport workspaceSupport;

    @MockBean
    private FirebaseAuth firebaseAuth;

    private String e2eId;
    private String sessionId;
    private String otherWorkspaceCustomerId;
    private String ownerAccessToken;

    @BeforeEach
    void setUp() throws Exception {
        e2eId = UUID.randomUUID().toString();
        sessionId = UUID.randomUUID().toString();
        testDatabaseCleaner.wipeAndSeedDefaultWorkspace(
                workspaceRepository, employeeRepository, workspaceSupport);

        workspaceRepository.save(new Workspace(OTHER_WORKSPACE_ID, "Other Business", "other-owner-uid"));
        Employee otherOwner = new Employee("other-owner-uid", "Other Owner", EmployeeRole.OWNER);
        otherOwner.setWorkspaceId(OTHER_WORKSPACE_ID);
        employeeRepository.save(otherOwner);

        String otherTerritoryId = "ter_other_" + UUID.randomUUID().toString().replace("-", "");
        Territory otherTerritory = new Territory(otherTerritoryId, "Other Village");
        otherTerritory.setWorkspaceId(OTHER_WORKSPACE_ID);
        territoryRepository.save(otherTerritory);

        GlobalPlan otherPlan = new GlobalPlan("plan-other", "Other Plan", new BigDecimal("199.00"), "SD");
        otherPlan.setWorkspaceId(OTHER_WORKSPACE_ID);
        otherPlan = globalPlanRepository.save(otherPlan);

        otherWorkspaceCustomerId = "cust_other_" + UUID.randomUUID().toString().replace("-", "");
        Customer otherCustomer = new Customer(
                otherWorkspaceCustomerId,
                1,
                "Other Customer",
                "9999999999",
                "Other Street",
                "1-1",
                new BigDecimal("199.00"),
                otherTerritory,
                otherPlan
        );
        otherCustomer.setWorkspaceId(OTHER_WORKSPACE_ID);
        customerRepository.save(otherCustomer);

        ownerAccessToken = jwtTokenService.createAccessToken(
                TestWorkspaceSupport.OWNER_UID, "ROLE_OWNER", TestWorkspaceSupport.WORKSPACE_ID);

        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn(TestWorkspaceSupport.OWNER_UID);
        when(firebaseToken.getClaims()).thenReturn(Map.of("role", "OWNER"));
        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(firebaseToken);
    }

    @Test
    void getCustomerProfile_crossWorkspace_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}", otherWorkspaceCustomerId)
                        .header("Authorization", "Bearer " + ownerAccessToken)
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isForbidden());
    }

    @Test
    void collectPayment_crossWorkspace_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/customers/{id}/payments", otherWorkspaceCustomerId)
                        .header("Authorization", "Bearer " + ownerAccessToken)
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 500,
                                  "monthsPaid": ["JAN"],
                                  "year": 2026
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
