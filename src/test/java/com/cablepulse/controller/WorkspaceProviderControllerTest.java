package com.cablepulse.controller;

import com.cablepulse.repository.ConnectionProviderRepository;
import com.cablepulse.repository.EmployeeRepository;
import com.cablepulse.repository.WorkspaceRepository;
import com.cablepulse.testsupport.TestWorkspaceSupport;
import com.cablepulse.repository.TerritoryRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkspaceProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConnectionProviderRepository connectionProviderRepository;

    @Autowired
    private TerritoryRepository territoryRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

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
        connectionProviderRepository.deleteAll();
        territoryRepository.deleteAll();
        workspaceRepository.deleteAll();
        employeeRepository.deleteAll();
        workspaceSupport.seedDefaultWorkspace();
        employeeRepository.save(workspaceSupport.ownerEmployee());

        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn("owner-uid");
        when(firebaseToken.getClaims()).thenReturn(Map.of("role", "OWNER"));
        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(firebaseToken);
    }

    @Test
    void createProvider_returnsCreatedForNewCategory() throws Exception {
        mockMvc.perform(post("/api/v1/workspace/providers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Skynet Cable Networks"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.name").value("Skynet Cable Networks"));
    }

    @Test
    void createTerritoryViaProviders_minimalBody_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/v1/workspace/providers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"location_name": "Kolamuru"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.location_name").value("Kolamuru"))
                .andExpect(jsonPath("$.data.territory_id").exists());
    }

    @Test
    void createTerritoryViaProviders_fullBody_returnsCreatedWithBlocks() throws Exception {
        mockMvc.perform(post("/api/v1/workspace/providers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "location_name": "Kolamuru",
                                  "blocks": ["Ramalayam Street", "School Road"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.location_name").value("Kolamuru"))
                .andExpect(jsonPath("$.data.territory_id").exists());
    }

    @Test
    void deleteTerritory_softDeletesActiveTerritory() throws Exception {
        mockMvc.perform(post("/api/v1/workspace/providers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"location_name": "Kolamuru"}
                                """))
                .andExpect(status().isCreated());

        String territoryId = territoryRepository.findAll().get(0).getTerritoryId();

        mockMvc.perform(delete("/api/v1/workspace/territories/" + territoryId)
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        mockMvc.perform(get("/api/v1/workspace/territories")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void deleteTerritory_withBlocks_softDeletesWithoutError() throws Exception {
        mockMvc.perform(post("/api/v1/workspace/providers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "location_name": "Kolamuru",
                                  "blocks": ["Ramalayam Street", "School Road"]
                                }
                                """))
                .andExpect(status().isCreated());

        String territoryId = territoryRepository.findAll().get(0).getTerritoryId();

        mockMvc.perform(delete("/api/v1/workspace/territories/" + territoryId)
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void getTerritoryBlocks_returnsSavedBlockNames() throws Exception {
        mockMvc.perform(post("/api/v1/workspace/providers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "location_name": "Kolamuru",
                                  "blocks": ["Ramalayam Street", "School Road"]
                                }
                                """))
                .andExpect(status().isCreated());

        String territoryId = territoryRepository.findAll().get(0).getTerritoryId();

        mockMvc.perform(get("/api/v1/workspace/territories/" + territoryId + "/blocks")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0]").value("Ramalayam Street"))
                .andExpect(jsonPath("$.data[1]").value("School Road"));
    }

    @Test
    void createProvider_returnsConflictWhenCategoryAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/v1/workspace/providers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Skynet Cable Networks"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/workspace/providers")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Skynet Cable Networks"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.error").value("Provider category already exists"))
                .andExpect(jsonPath("$.data.name").value("Skynet Cable Networks"));
    }
}
