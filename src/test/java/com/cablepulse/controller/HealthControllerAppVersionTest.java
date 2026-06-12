package com.cablepulse.controller;

import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
            "cablepulse.app.latest-stable-version=1.0.4",
            "cablepulse.app.minimum-required-version=1.0.1",
            "cablepulse.app.is-critical-patch=true"
        })
class HealthControllerAppVersionTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private FirebaseAuth firebaseAuth;

    @Test
    void appVersionCheckReturnsConfiguredRulesWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/v1/auth/app-version-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latest_stable_version").value("1.0.4"))
                .andExpect(jsonPath("$.minimum_required_version").value("1.0.1"))
                .andExpect(jsonPath("$.is_critical_patch").value(true));
    }
}
