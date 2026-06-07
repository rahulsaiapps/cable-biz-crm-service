package com.cablepulse.exception;

import com.cablepulse.infrastructure.WebHeaderInterceptor;
import com.cablepulse.service.DailyLedgerService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class GlobalExceptionHandlerEdgeCaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private DailyLedgerService dailyLedgerService;

    private String e2eId;
    private String sessionId;

    @BeforeEach
    void setUp() throws Exception {
        e2eId = UUID.randomUUID().toString();
        sessionId = UUID.randomUUID().toString();

        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn("test-user-id");
        when(firebaseToken.getClaims()).thenReturn(Map.of("role", "OWNER"));
        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(firebaseToken);
    }

    @Test
    void whenInvalidEnumProvided_returnsBadRequestAndStructuredJson() throws Exception {
        String invalidPayload = """
                {
                    "amount": 500.0,
                    "description": "Coaxial Wire 100m Coil Purchase",
                    "expenseCategory": "CABLE_WIRE_TYPO"
                }
                """;

        mockMvc.perform(post("/api/v1/transactions/expense")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload)
                        .header("Authorization", "Bearer valid-token")
                        .header(WebHeaderInterceptor.E2E_ID_HEADER, e2eId)
                        .header(WebHeaderInterceptor.SESSION_ID_HEADER, sessionId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void whenInvalidDateFormatProvided_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/daily-summary")
                        .param("targetDate", "07-06-2026")
                        .header("Authorization", "Bearer valid-token")
                        .header(WebHeaderInterceptor.E2E_ID_HEADER, e2eId)
                        .header(WebHeaderInterceptor.SESSION_ID_HEADER, sessionId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void whenDatabaseFailureOrUnexpectedCrash_returnsInternalServerErrorWithStandardizedEnvelope() throws Exception {
        when(dailyLedgerService.getDailySummary(any())).thenThrow(
                new TransientDataAccessResourceException("Database connection timed out")
        );

        mockMvc.perform(get("/api/v1/transactions/daily-summary")
                        .param("targetDate", "2026-06-07")
                        .header("Authorization", "Bearer valid-token")
                        .header(WebHeaderInterceptor.E2E_ID_HEADER, e2eId)
                        .header(WebHeaderInterceptor.SESSION_ID_HEADER, sessionId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.error").value("An unexpected error occurred. Please contact administrator."))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
