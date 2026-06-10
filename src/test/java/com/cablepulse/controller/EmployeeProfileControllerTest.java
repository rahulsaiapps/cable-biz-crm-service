package com.cablepulse.controller;

import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.repository.EmployeeRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeRepository employeeRepository;

    @MockBean
    private FirebaseAuth firebaseAuth;

    private String e2eId;
    private String sessionId;

    @BeforeEach
    void setUp() throws Exception {
        e2eId = UUID.randomUUID().toString();
        sessionId = UUID.randomUUID().toString();
        employeeRepository.deleteAll();

        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn("firebase-uid-123");
        when(firebaseToken.getClaims()).thenReturn(Map.of());
        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(firebaseToken);
    }

    @Test
    void updateProfile_appliesOnlyProvidedFieldsForAuthenticatedEmployee() throws Exception {
        Employee employee = new Employee("firebase-uid-123", "Ramesh Kumar", EmployeeRole.COLLECTION_BOY);
        employee.setEmail("old@example.com");
        employee.setDescription("Old caption");
        employeeRepository.save(employee);

        mockMvc.perform(patch("/api/v1/employees/profile")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "full_name": "Ramesh K.",
                                  "description": "Field agent for Kolamuru"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.employee_id").value("firebase-uid-123"))
                .andExpect(jsonPath("$.data.full_name").value("Ramesh K."))
                .andExpect(jsonPath("$.data.email").value("old@example.com"))
                .andExpect(jsonPath("$.data.description").value("Field agent for Kolamuru"))
                .andExpect(jsonPath("$.data.role").value("COLLECTION_BOY"));

        Employee updated = employeeRepository.findById("firebase-uid-123").orElseThrow();
        assertThat(updated.getFullName()).isEqualTo("Ramesh K.");
        assertThat(updated.getEmail()).isEqualTo("old@example.com");
        assertThat(updated.getDescription()).isEqualTo("Field agent for Kolamuru");
    }

    @Test
    void updateProfile_returnsNotFoundWhenPrincipalHasNoEmployeeRow() throws Exception {
        mockMvc.perform(patch("/api/v1/employees/profile")
                        .header("Authorization", "Bearer test-token")
                        .header("X-E2E-ID", e2eId)
                        .header("X-Session-ID", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new@example.com"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }
}
