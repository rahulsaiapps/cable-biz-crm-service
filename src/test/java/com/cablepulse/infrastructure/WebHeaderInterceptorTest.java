package com.cablepulse.infrastructure;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(WebHeaderInterceptorTest.MdcProbeConfig.class)
class WebHeaderInterceptorTest {

    static final AtomicReference<String> MDC_SNAPSHOT = new AtomicReference<>();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @BeforeEach
    void setUpFirebaseAuth() throws Exception {
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn("integration-test-user");
        when(firebaseToken.getClaims()).thenReturn(Map.of("role", "OWNER"));
        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(firebaseToken);
        MDC_SNAPSHOT.set(null);
        MDC.clear();
    }

    @AfterEach
    void tearDownMdc() {
        MDC.clear();
        MDC_SNAPSHOT.set(null);
    }

    @Test
    void happyPath_withValidTracingHeaders_returnsOk() throws Exception {
        UUID e2eId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer valid-test-token")
                        .header(WebHeaderInterceptor.E2E_ID_HEADER, e2eId.toString())
                        .header(WebHeaderInterceptor.SESSION_ID_HEADER, sessionId.toString())
                        .with(user("owner").roles("OWNER")))
                .andExpect(status().isOk());

        assertThat(MDC_SNAPSHOT.get()).isEqualTo(e2eId.toString());
        assertThat(MDC.get(WebHeaderInterceptor.MDC_E2E_ID_KEY)).isNull();
    }

    @Test
    void missingE2eIdHeader_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer valid-test-token")
                        .header(WebHeaderInterceptor.SESSION_ID_HEADER, UUID.randomUUID().toString())
                        .with(user("owner").roles("OWNER")))
                .andExpect(status().isBadRequest());

        assertThat(MDC_SNAPSHOT.get()).isNull();
        assertThat(MDC.get(WebHeaderInterceptor.MDC_E2E_ID_KEY)).isNull();
    }

    @Test
    void malformedSessionIdHeader_failsAuthenticationControls() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer valid-test-token")
                        .header(WebHeaderInterceptor.E2E_ID_HEADER, UUID.randomUUID().toString())
                        .header(WebHeaderInterceptor.SESSION_ID_HEADER, "not-a-valid-uuid")
                        .with(user("owner").roles("OWNER")))
                .andExpect(status().isUnauthorized());

        assertThat(MDC_SNAPSHOT.get()).isNull();
        assertThat(MDC.get(WebHeaderInterceptor.MDC_E2E_ID_KEY)).isNull();
    }

    @Test
    void mdcContext_isClearedAfterRequestCompletion() throws Exception {
        UUID e2eId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer valid-test-token")
                        .header(WebHeaderInterceptor.E2E_ID_HEADER, e2eId.toString())
                        .header(WebHeaderInterceptor.SESSION_ID_HEADER, UUID.randomUUID().toString())
                        .with(user("owner").roles("OWNER")))
                .andExpect(status().isOk());

        assertThat(MDC.get(WebHeaderInterceptor.MDC_E2E_ID_KEY)).isNull();
    }

    @TestConfiguration
    static class MdcProbeConfig implements WebMvcConfigurer {

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(new HandlerInterceptor() {
                        @Override
                        public boolean preHandle(
                                HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler) {
                            MDC_SNAPSHOT.set(MDC.get(WebHeaderInterceptor.MDC_E2E_ID_KEY));
                            return true;
                        }
                    })
                    .addPathPatterns("/api/v1/**")
                    .excludePathPatterns("/api/v1/auth/**")
                    .order(1);
        }
    }
}
