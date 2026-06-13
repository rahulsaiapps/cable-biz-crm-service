package com.cablepulse.controller;

import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.repository.EmployeeRepository;
import com.cablepulse.repository.WorkspaceRepository;
import com.cablepulse.security.JwtTokenService;
import com.cablepulse.testsupport.TestDatabaseCleaner;
import com.cablepulse.testsupport.TestWorkspaceSupport;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerRefreshTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

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

    @BeforeEach
    void setUp() {
        testDatabaseCleaner.wipeAndSeedDefaultWorkspace(
                workspaceRepository, employeeRepository, workspaceSupport);
    }

    @Test
    void refresh_validToken_returnsNewAccessToken() throws Exception {
        String refreshToken = jwtTokenService.createRefreshToken(TestWorkspaceSupport.OWNER_UID);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.userProfile.workspaceId").value(TestWorkspaceSupport.WORKSPACE_ID));
    }

    @Test
    void refresh_deletedEmployee_returnsUnauthorized() throws Exception {
        String deletedUid = "deleted-uid";
        String refreshToken = jwtTokenService.createRefreshToken(deletedUid);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Account no longer exists. Please sign in again."));
    }

    @Test
    void refresh_accessTokenRejected() throws Exception {
        String accessToken = jwtTokenService.createAccessToken(
                TestWorkspaceSupport.OWNER_UID, "ROLE_OWNER", TestWorkspaceSupport.WORKSPACE_ID);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}
                                """.formatted(accessToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid refresh token"));
    }
}
