package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.infrastructure.WebHeaderInterceptor;
import com.cablepulse.model.Customer;
import com.cablepulse.model.GlobalPlan;
import com.cablepulse.model.Territory;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.TerritoryRepository;
import com.cablepulse.service.DailyLedgerService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
public class TransactionControllerHappyPathTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FirebaseAuth firebaseAuth;

    @MockBean
    private CustomerRepository customerRepository;

    @MockBean
    private TerritoryRepository territoryRepository;

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
    void workspaceCustomerFlow_includesOptionalHardwareTrackingFields() throws Exception {
        Territory territory = new Territory("vil_kolamuru_001", "Kolamuru");
        GlobalPlan plan = new GlobalPlan("plan-001", "Pro Pack", new BigDecimal("199.00"), List.of("HD"));
        Customer customer = new Customer(
                "cust-001",
                1,
                "Satish Kumar",
                "9876543210",
                "Block A",
                "Door 1",
                new BigDecimal("199.00"),
                territory,
                plan,
                "AP Fiber",
                "STB987654",
                "VC11223344"
        );

        when(territoryRepository.findById("vil_kolamuru_001")).thenReturn(Optional.of(territory));
        when(customerRepository.findByTerritory_TerritoryId("vil_kolamuru_001")).thenReturn(List.of(customer));

        mockMvc.perform(get("/api/v1/workspace/customers")
                        .param("locationId", "vil_kolamuru_001")
                        .header("Authorization", "Bearer valid-token")
                        .header(WebHeaderInterceptor.E2E_ID_HEADER, e2eId)
                        .header(WebHeaderInterceptor.SESSION_ID_HEADER, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customers[0].connectionType").value("AP Fiber"))
                .andExpect(jsonPath("$.data.customers[0].boxNumber").value("STB987654"))
                .andExpect(jsonPath("$.data.customers[0].cardNumber").value("VC11223344"));
    }

    @Test
    void recordExpenseAndSettlementFlow() throws Exception {
        String expensePayload = """
                {
                    "amount": 500.0,
                    "description": "Coaxial Wire 100m Coil Purchase",
                    "expenseCategory": "WIRE"
                }
                """;

        when(dailyLedgerService.saveExpense(any())).thenReturn(
                new StandardResponse_ExpenseCreated(LocalDateTime.now(), "SUCCESS", null, new ExpenseCreatedData(1L))
        );

        mockMvc.perform(post("/api/v1/transactions/expense")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expensePayload)
                        .header("Authorization", "Bearer valid-token")
                        .header(WebHeaderInterceptor.E2E_ID_HEADER, e2eId)
                        .header(WebHeaderInterceptor.SESSION_ID_HEADER, sessionId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.expenseId").value(1L));

        String settlementPayload = """
                {
                    "connectionTypeName": "JIO",
                    "amountPaid": 4500.0,
                    "paymentStatus": "PARTIAL_PAYMENT",
                    "settlementNotes": "Cleared part of May line rental cycle fees"
                }
                """;

        when(dailyLedgerService.saveIspSettlement(any())).thenReturn(
                new StandardResponse_SettlementCreated(LocalDateTime.now(), "SUCCESS", null, new SettlementCreatedData(2L))
        );

        mockMvc.perform(post("/api/v1/transactions/isp-settlement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settlementPayload)
                        .header("Authorization", "Bearer valid-token")
                        .header(WebHeaderInterceptor.E2E_ID_HEADER, e2eId)
                        .header(WebHeaderInterceptor.SESSION_ID_HEADER, sessionId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.settlementId").value(2L));
    }

    @Test
    void cashDrawerDailySummaryArithmeticVerification() throws Exception {
        BigDecimal collected = BigDecimal.valueOf(12450.00);
        BigDecimal expensed = BigDecimal.valueOf(700.00);
        BigDecimal settlements = BigDecimal.valueOf(4500.00);
        BigDecimal netCash = collected.subtract(expensed).subtract(settlements); // 7250.00

        when(dailyLedgerService.getDailySummary(LocalDate.of(2026, 6, 7))).thenReturn(
                new StandardResponse_DailyCashSummaryData(
                        LocalDateTime.now(),
                        "SUCCESS",
                        null,
                        new DailyCashSummary(collected, expensed, settlements, netCash)
                )
        );

        mockMvc.perform(get("/api/v1/transactions/daily-summary")
                        .param("targetDate", "2026-06-07")
                        .header("Authorization", "Bearer valid-token")
                        .header(WebHeaderInterceptor.E2E_ID_HEADER, e2eId)
                        .header(WebHeaderInterceptor.SESSION_ID_HEADER, sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCollectedToday").value(collected.doubleValue()))
                .andExpect(jsonPath("$.data.totalExpensedToday").value(expensed.doubleValue()))
                .andExpect(jsonPath("$.data.totalIspSettlementsToday").value(settlements.doubleValue()))
                .andExpect(jsonPath("$.data.netCashInHand").value(netCash.doubleValue()))
                .andExpect(jsonPath("$.data.netCashInHand").value(collected.subtract(expensed).subtract(settlements).doubleValue()));
    }
}
