package com.cablepulse.controller;

import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.model.Territory;
import com.cablepulse.model.Customer;
import com.cablepulse.repository.ConnectionProviderRepository;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.EmployeeRepository;
import com.cablepulse.repository.TerritoryRepository;
import com.cablepulse.repository.WorkspaceRepository;
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

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerControllerCreateTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TerritoryRepository territoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ConnectionProviderRepository connectionProviderRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private TestWorkspaceSupport workspaceSupport;

    @MockBean
    private FirebaseAuth firebaseAuth;

    private String e2eId;
    private String sessionId;
    private String territoryId;

    @BeforeEach
    void setUp() throws Exception {
        e2eId = UUID.randomUUID().toString();
        sessionId = UUID.randomUUID().toString();
        testDatabaseCleaner.wipeAndSeedDefaultWorkspace(
                workspaceRepository, employeeRepository, workspaceSupport);

        territoryId = "ter_" + UUID.randomUUID().toString().replace("-", "");
        territoryRepository.save(workspaceSupport.territory(territoryId, "Kolamuru"));

        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn("owner-uid");
        when(firebaseToken.getClaims()).thenReturn(Map.of("role", "OWNER"));
        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(firebaseToken);
    }

    @Test
    void createCustomer_minimalBody_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rahul Test",
                                  "territory_id": "%s",
                                  "territory_name": "Kolamuru",
                                  "plan_name": "Custom Plan Rate",
                                  "plan_monthly_rate": 250
                                }
                                """.formatted(territoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.newCustomerId").exists());
    }

    @Test
    void createCustomer_fullOptionalFields_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rahul Test",
                                  "territory_id": "%s",
                                  "territory_name": "Kolamuru",
                                  "phone_number": "9876543210",
                                  "street": "Ramalayam Street",
                                  "door_number": "4-12/A",
                                  "plan_name": "Custom Plan Rate",
                                  "plan_monthly_rate": 250
                                }
                                """.formatted(territoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.newCustomerId").exists());
    }

    @Test
    void createCustomer_collectionBoyRole_returnsCreated() throws Exception {
        FirebaseToken collectionBoyToken = mock(FirebaseToken.class);
        when(collectionBoyToken.getUid()).thenReturn("collection-boy-uid");
        when(collectionBoyToken.getClaims()).thenReturn(Map.of("role", "COLLECTION_BOY"));
        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(collectionBoyToken);

        Employee collectionBoy = new Employee("collection-boy-uid", "Field Agent", EmployeeRole.COLLECTION_BOY);
        collectionBoy.setAssignedVillages(java.util.List.of("Kolamuru"));
        collectionBoy.setWorkspaceId(TestWorkspaceSupport.WORKSPACE_ID);
        employeeRepository.save(collectionBoy);

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Field Agent Customer",
                                  "territory_id": "%s",
                                  "territory_name": "Kolamuru",
                                  "plan_name": "Basic Pack",
                                  "plan_monthly_rate": 150
                                }
                                """.formatted(territoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.newCustomerId").exists());
    }

    @Test
    void createCustomer_withoutPlan_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "No Plan Customer",
                                  "territory_id": "%s",
                                  "territory_name": "Kolamuru"
                                }
                                """.formatted(territoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.newCustomerId").exists());
    }

    @Test
    void createCustomer_secondCustomerInSameTerritory_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "First Customer",
                                  "territory_id": "%s",
                                  "territory_name": "Kolamuru"
                                }
                                """.formatted(territoryId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Second Customer Different Name",
                                  "territory_id": "%s",
                                  "territory_name": "Kolamuru"
                                }
                                """.formatted(territoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.newCustomerId").exists());
    }

    /** Reproduces production Supabase bug: serial 1 exists in another territory. */
    @Test
    void createCustomer_whenSerialOneTakenInOtherTerritory_returnsCreated() throws Exception {
        String otherTerritoryId = "ter_" + UUID.randomUUID().toString().replace("-", "");
        Territory other = territoryRepository.save(
                workspaceSupport.territory(otherTerritoryId, "Other Village"));
        Customer existing = new Customer(
                "cust_existing_serial_1",
                1,
                "Existing Elsewhere",
                null,
                null,
                null,
                null,
                other,
                null);
        existing.setWorkspaceId(TestWorkspaceSupport.WORKSPACE_ID);
        customerRepository.save(existing);

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rahul",
                                  "territory_id": "%s",
                                  "territory_name": "Kolamuru"
                                }
                                """.formatted(territoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.newCustomerId").exists());
    }

    @Test
    void listTerritories_afterCreateCustomer_returnsAccurateCounts() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Counted Customer",
                                  "territory_id": "%s",
                                  "territory_name": "Kolamuru"
                                }
                                """.formatted(territoryId)))
                .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/workspace/territories")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].customer_count").value(1))
                .andExpect(jsonPath("$.data[0].active_count").value(0))
                .andExpect(jsonPath("$.data[0].pending_count").value(1));
    }

    @Test
    void deleteCustomer_softDeletesAndHidesFromWorkspaceList() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "To Delete",
                                  "territory_id": "%s",
                                  "territory_name": "Kolamuru"
                                }
                                """.formatted(territoryId)))
                .andExpect(status().isCreated());

        String customerId = customerRepository.findAll().get(0).getCustomerId();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/v1/customers/" + customerId)
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/customers/" + customerId)
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isNotFound());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/workspace/customers")
                        .param("locationId", territoryId)
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customers").isEmpty());
    }
}
