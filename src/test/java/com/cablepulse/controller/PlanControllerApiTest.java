package com.cablepulse.controller;

import com.cablepulse.model.ConnectionProvider;
import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.model.GlobalPlan;
import com.cablepulse.repository.ConnectionProviderRepository;
import com.cablepulse.repository.EmployeeRepository;
import com.cablepulse.repository.GlobalPlanRepository;
import com.cablepulse.testsupport.TestDatabaseCleaner;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API coverage for {@code GET/POST /api/v1/plans}, including DB assertions
 * on {@code global_plans} and {@code connection_providers}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PlanControllerApiTest {

    private static final String PROVIDER_AIRTEL = "airtel";
    private static final String PROVIDER_JIO = "jio";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConnectionProviderRepository connectionProviderRepository;

    @Autowired
    private GlobalPlanRepository globalPlanRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private FirebaseAuth firebaseAuth;

    private String e2eId;
    private String sessionId;

    @BeforeEach
    void setUp() throws Exception {
        e2eId = UUID.randomUUID().toString();
        sessionId = UUID.randomUUID().toString();
        testDatabaseCleaner.wipeCoreWorkspaceData();
        connectionProviderRepository.save(new ConnectionProvider(PROVIDER_AIRTEL));
        connectionProviderRepository.save(new ConnectionProvider(PROVIDER_JIO));
        mockOwnerToken();
    }

    @Nested
    @DisplayName("POST /api/v1/plans")
    class PostPlans {

        @Test
        @DisplayName("201 — full payload persists all fields in global_plans")
        void createPlan_fullPayload_persistsToDatabase() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/v1/plans")
                            .header("Authorization", "Bearer owner-token")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Kids 23",
                                      "price": 356,
                                      "channels_text": "100+ Channels Included",
                                      "is_hd": false,
                                      "provider": "airtel"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.createdPlanId").isNotEmpty())
                    .andReturn();

            String createdPlanId = readCreatedPlanId(result);

            assertPlanRowInDatabase(
                    createdPlanId,
                    "Kids 23",
                    PROVIDER_AIRTEL,
                    new BigDecimal("356.00"),
                    "100+ Channels Included",
                    false);

            var entity = globalPlanRepository.findById(createdPlanId).orElseThrow();
            assertThat(entity.getPlanName()).isEqualTo("Kids 23");
        }

        @Test
        @DisplayName("201 — minimal payload (optional fields omitted)")
        void createPlan_minimalPayload_persistsDefaults() throws Exception {
            mockMvc.perform(post("/api/v1/plans")
                            .header("Authorization", "Bearer owner-token")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Silver SD Pack",
                                      "price": 220,
                                      "provider": "airtel"
                                    }
                                    """))
                    .andExpect(status().isCreated());

            var plans = globalPlanRepository.findByProvider_Name(PROVIDER_AIRTEL);
            assertThat(plans).hasSize(1);
            assertThat(plans.get(0).getChannelsText()).isNull();
            assertThat(plans.get(0).getHd()).isFalse();

            assertPlanCountInDatabase(1);
        }

        @Test
        @DisplayName("404 — unknown provider category is not saved")
        void createPlan_unknownProvider_returnsNotFoundAndNoDbRow() throws Exception {
            mockMvc.perform(post("/api/v1/plans")
                            .header("Authorization", "Bearer owner-token")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Ghost Plan",
                                      "price": 100,
                                      "provider": "nonexistent-isp"
                                    }
                                    """))
                    .andExpect(status().isNotFound());

            assertPlanCountInDatabase(0);
        }

        @Test
        @DisplayName("400 — missing required name")
        void createPlan_missingName_returnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/plans")
                            .header("Authorization", "Bearer owner-token")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "price": 100,
                                      "provider": "airtel"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            assertPlanCountInDatabase(0);
        }

        @Test
        @DisplayName("400 — non-positive price")
        void createPlan_zeroPrice_returnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/plans")
                            .header("Authorization", "Bearer owner-token")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Free Plan",
                                      "price": 0,
                                      "provider": "airtel"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            assertPlanCountInDatabase(0);
        }

        @Test
        @DisplayName("400 — missing provider")
        void createPlan_missingProvider_returnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/plans")
                            .header("Authorization", "Bearer owner-token")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Orphan Plan",
                                      "price": 199
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            assertPlanCountInDatabase(0);
        }

        @Test
        @DisplayName("403 — no bearer token (Spring Security rejects before controller)")
        void createPlan_unauthenticated_returnsForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/plans")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Kids 23",
                                      "price": 356,
                                      "provider": "airtel"
                                    }
                                    """))
                    .andExpect(status().isForbidden());

            assertPlanCountInDatabase(0);
        }

        @Test
        @DisplayName("403 — COLLECTION_BOY cannot create plans")
        void createPlan_collectionBoyRole_returnsForbidden() throws Exception {
            mockCollectionBoyToken();

            mockMvc.perform(post("/api/v1/plans")
                            .header("Authorization", "Bearer boy-token")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Kids 23",
                                      "price": 356,
                                      "provider": "airtel"
                                    }
                                    """))
                    .andExpect(status().isForbidden());

            assertPlanCountInDatabase(0);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/plans")
    class GetPlans {

        @Test
        @DisplayName("200 — empty list when no plans exist")
        void getPlans_noData_returnsEmptyList() throws Exception {
            mockMvc.perform(get("/api/v1/plans")
                            .header("Authorization", "Bearer owner-token")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("200 — returns all plans matching database rows")
        void getPlans_allProviders_returnsEverySavedPlan() throws Exception {
            seedPlan("Kids 23", PROVIDER_AIRTEL, 356, "100+ Channels Included");
            seedPlan("Jio Max", PROVIDER_JIO, 299, "Sports + Movies");

            mockMvc.perform(get("/api/v1/plans")
                            .header("Authorization", "Bearer owner-token")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[?(@.name == 'Kids 23')]").exists())
                    .andExpect(jsonPath("$.data[?(@.name == 'Jio Max')]").exists());

            assertPlanCountInDatabase(2);
        }

        @Test
        @DisplayName("200 — providerName filter returns only matching category")
        void getPlans_byProvider_filtersCorrectly() throws Exception {
            seedPlan("Kids 23", PROVIDER_AIRTEL, 356, "100+ Channels Included");
            seedPlan("Jio Max", PROVIDER_JIO, 299, "Sports + Movies");

            mockMvc.perform(get("/api/v1/plans")
                            .param("providerName", PROVIDER_AIRTEL)
                            .header("Authorization", "Bearer owner-token")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Kids 23"))
                    .andExpect(jsonPath("$.data[0].price").value(356))
                    .andExpect(jsonPath("$.data[0].channels_text").value("100+ Channels Included"))
                    .andExpect(jsonPath("$.data[0].provider").value(PROVIDER_AIRTEL));
        }

        @Test
        @DisplayName("200 — unknown providerName returns empty list (not 404)")
        void getPlans_unknownProvider_returnsEmptyList() throws Exception {
            seedPlan("Kids 23", PROVIDER_AIRTEL, 356, null);

            mockMvc.perform(get("/api/v1/plans")
                            .param("providerName", "unknown-isp")
                            .header("Authorization", "Bearer owner-token")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));

            assertPlanCountInDatabase(1);
        }

        @Test
        @DisplayName("200 — POST then GET round-trip matches DB")
        void postThenGet_returnsCreatedPlanFromDatabase() throws Exception {
            MvcResult postResult = mockMvc.perform(post("/api/v1/plans")
                            .header("Authorization", "Bearer owner-token")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "Kids 23",
                                      "price": 356,
                                      "channels_text": "100+ Channels Included",
                                      "provider": "airtel"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andReturn();

            String createdPlanId = readCreatedPlanId(postResult);

            mockMvc.perform(get("/api/v1/plans")
                            .param("providerName", PROVIDER_AIRTEL)
                            .header("Authorization", "Bearer owner-token")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].id").value(createdPlanId))
                    .andExpect(jsonPath("$.data[0].name").value("Kids 23"))
                    .andExpect(jsonPath("$.data[0].price").value(356));

            assertPlanRowInDatabase(
                    createdPlanId,
                    "Kids 23",
                    PROVIDER_AIRTEL,
                    new BigDecimal("356.00"),
                    "100+ Channels Included",
                    false);
        }

        @Test
        @DisplayName("403 — no bearer token (Spring Security rejects before controller)")
        void getPlans_unauthenticated_returnsForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/plans")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("200 — COLLECTION_BOY can list plans (GET has no @PreAuthorize OWNER)")
        void getPlans_collectionBoyRole_returnsOk() throws Exception {
            mockCollectionBoyToken();
            seedPlan("Kids 23", PROVIDER_AIRTEL, 356, null);

            mockMvc.perform(get("/api/v1/plans")
                            .header("Authorization", "Bearer boy-token")
                            .header("X-E2E-ID", e2eId)
                            .header("X-Session-ID", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Kids 23"));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void mockOwnerToken() throws Exception {
        FirebaseToken ownerToken = mock(FirebaseToken.class);
        when(ownerToken.getUid()).thenReturn("owner-uid");
        when(ownerToken.getClaims()).thenReturn(Map.of("role", "OWNER"));
        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(ownerToken);
    }

    private void mockCollectionBoyToken() throws Exception {
        FirebaseToken boyToken = mock(FirebaseToken.class);
        when(boyToken.getUid()).thenReturn("collection-boy-uid");
        when(boyToken.getClaims()).thenReturn(Map.of("role", "COLLECTION_BOY"));
        when(firebaseAuth.verifyIdToken(anyString())).thenReturn(boyToken);

        Employee collectionBoy =
                new Employee("collection-boy-uid", "Field Agent", EmployeeRole.COLLECTION_BOY);
        employeeRepository.save(collectionBoy);
    }

    private GlobalPlan seedPlan(
            String name, String providerName, int price, String channelsText) {
        ConnectionProvider provider = connectionProviderRepository.findByName(providerName)
                .orElseThrow();
        GlobalPlan plan = new GlobalPlan();
        plan.setPlanId("plan-" + UUID.randomUUID());
        plan.setPlanName(name);
        plan.setMonthlyRate(BigDecimal.valueOf(price));
        plan.setChannelsText(channelsText);
        plan.setProvider(provider);
        return globalPlanRepository.save(plan);
    }

    private static String readCreatedPlanId(MvcResult result) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.data.createdPlanId");
    }

    private void assertPlanCountInDatabase(int expected) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM global_plans", Integer.class);
        assertThat(count).isEqualTo(expected);
    }

    private void assertPlanRowInDatabase(
            String planId,
            String planName,
            String providerName,
            BigDecimal monthlyRate,
            String channelsText,
            boolean isHd) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM global_plans gp
                JOIN connection_providers cp ON gp.provider_id = cp.id
                WHERE gp.plan_id = ?
                  AND gp.plan_name = ?
                  AND cp.name = ?
                  AND gp.monthly_rate = ?
                  AND (gp.channels_text = ? OR (gp.channels_text IS NULL AND ? IS NULL))
                  AND gp.is_hd = ?
                """,
                Integer.class,
                planId,
                planName,
                providerName,
                monthlyRate,
                channelsText,
                channelsText,
                isHd);
        assertThat(count)
                .as("Expected plan row planId=%s name=%s provider=%s", planId, planName, providerName)
                .isEqualTo(1);
    }
}
