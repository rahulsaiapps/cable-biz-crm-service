package com.cablepulse.controller;

import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.repository.EmployeeRepository;
import com.cablepulse.repository.TerritoryRepository;
import com.cablepulse.repository.WorkspaceRepository;
import com.cablepulse.testsupport.TestWorkspaceSupport;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.hamcrest.Matchers;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private TerritoryRepository territoryRepository;

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
        employeeRepository.deleteAll();
        territoryRepository.deleteAll();
        workspaceRepository.deleteAll();
        workspaceSupport.seedDefaultWorkspace();
        employeeRepository.save(workspaceSupport.ownerEmployee());

        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn("owner-uid");
        when(firebaseToken.getClaims()).thenReturn(Map.of("role", "OWNER"));
        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(firebaseToken);
    }

    @Test
    void listEmployees_returnsSnakeCaseRosterForFlutter() throws Exception {
        Employee employee = new Employee("emp-001", "Ramesh Kumar", EmployeeRole.COLLECTION_BOY);
        employee.setEmail("ramesh@example.com");
        employee.setWorkspaceId(TestWorkspaceSupport.WORKSPACE_ID);
        employeeRepository.save(employee);

        mockMvc.perform(get("/api/v1/employees")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data[?(@.employee_id == 'emp-001')]").exists())
                .andExpect(jsonPath("$.data[?(@.employee_id == 'emp-001')].full_name").value("Ramesh Kumar"))
                .andExpect(jsonPath("$.data[?(@.employee_id == 'emp-001')].role").value("COLLECTION_BOY"))
                .andExpect(jsonPath("$.data[?(@.employee_id == 'emp-001')].email").value("ramesh@example.com"))
                .andExpect(jsonPath("$.data[?(@.employee_id == 'emp-001')].today_collection").value(0));
    }

    @Test
    void createEmployee_acceptsSnakeCaseFullName() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "full_name": "Satish Kumar",
                                  "role": "COLLECTION_BOY",
                                  "email": "satish@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.full_name").value("Satish Kumar"))
                .andExpect(jsonPath("$.data.employee_id").value(Matchers.startsWith("PENDING-")));
    }

    @Test
    void getEmployee_returnsDetail() throws Exception {
        Employee employee = new Employee("emp-detail", "Detail Agent", EmployeeRole.COLLECTION_BOY);
        employee.setEmail("detail@example.com");
        employee.setWorkspaceId(TestWorkspaceSupport.WORKSPACE_ID);
        employeeRepository.save(employee);

        mockMvc.perform(get("/api/v1/employees/emp-detail")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employee_id").value("emp-detail"))
                .andExpect(jsonPath("$.data.full_name").value("Detail Agent"));
    }

    @Test
    void deleteEmployee_removesCollector() throws Exception {
        Employee employee = new Employee("emp-delete", "Delete Me", EmployeeRole.COLLECTION_BOY);
        employee.setWorkspaceId(TestWorkspaceSupport.WORKSPACE_ID);
        employeeRepository.save(employee);

        mockMvc.perform(delete("/api/v1/employees/emp-delete")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/employees/emp-delete")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateEmployee_assignsVillages() throws Exception {
        territoryRepository.save(workspaceSupport.territory("terr-1", "Kolamuru"));

        Employee employee = new Employee("emp-patch", "Patch Agent", EmployeeRole.COLLECTION_BOY);
        employee.setEmail("patch@example.com");
        employee.setWorkspaceId(TestWorkspaceSupport.WORKSPACE_ID);
        employeeRepository.save(employee);

        mockMvc.perform(patch("/api/v1/employees/emp-patch")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assigned_villages": ["Kolamuru"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigned_villages[0]").value("Kolamuru"));
    }

    @Test
    void deleteEmployee_rejectsOwner() throws Exception {
        mockMvc.perform(delete("/api/v1/employees/" + TestWorkspaceSupport.OWNER_UID)
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(Matchers.containsString("Owner")));
    }

    @Test
    void updateEmployee_rejectsOwner() throws Exception {
        mockMvc.perform(patch("/api/v1/employees/" + TestWorkspaceSupport.OWNER_UID)
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assigned_villages": ["Kolamuru"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(Matchers.containsString("Owner")));
    }

    @Test
    void createEmployee_rejectsOwnerRole() throws Exception {
        mockMvc.perform(post("/api/v1/employees")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "full_name": "Fake Owner",
                                  "role": "OWNER",
                                  "email": "fake-owner@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(Matchers.containsString("OWNER")));
    }
}
