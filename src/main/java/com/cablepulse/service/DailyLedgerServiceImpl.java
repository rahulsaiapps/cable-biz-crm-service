package com.cablepulse.service;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.model.*;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.DailyExpenseRepository;
import com.cablepulse.repository.DailyTransactionRepository;
import com.cablepulse.repository.EmployeeRepository;
import com.cablepulse.repository.IspSettlementRepository;
import com.cablepulse.security.SecurityAuth;
import com.cablepulse.security.WorkspaceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DailyLedgerServiceImpl implements DailyLedgerService {

    private final DailyExpenseRepository dailyExpenseRepository;
    private final DailyTransactionRepository dailyTransactionRepository;
    private final IspSettlementRepository ispSettlementRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final PaymentProcessingService paymentProcessingService;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    public DailyLedgerServiceImpl(
            DailyExpenseRepository dailyExpenseRepository,
            DailyTransactionRepository dailyTransactionRepository,
            IspSettlementRepository ispSettlementRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            PaymentProcessingService paymentProcessingService,
            WorkspaceAuthorizationService workspaceAuthorizationService) {
        this.dailyExpenseRepository = dailyExpenseRepository;
        this.dailyTransactionRepository = dailyTransactionRepository;
        this.ispSettlementRepository = ispSettlementRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.paymentProcessingService = paymentProcessingService;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    @Override
    @Transactional
    public StandardResponse_ExpenseCreated saveExpense(DailyExpense expense) {
        validateExpense(expense);
        expense.setId(null);
        if (expense.getLoggedAt() == null) {
            expense.setLoggedAt(LocalDateTime.now());
        }
        String loggedBy = SecurityAuth.currentUserId();
        if (loggedBy != null && !loggedBy.isBlank()) {
            expense.setLoggedByEmployeeId(loggedBy);
        }
        expense.setWorkspaceId(SecurityAuth.requireWorkspaceId());

        DailyExpense saved = dailyExpenseRepository.save(expense);
        return new StandardResponse_ExpenseCreated(
                LocalDateTime.now(),
                "CREATED",
                null,
                new ExpenseCreatedData(saved.getId())
        );
    }

    @Override
    @Transactional
    public StandardResponse_SettlementCreated saveIspSettlement(IspSettlement settlement) {
        validateSettlement(settlement);
        settlement.setId(null);
        if (settlement.getTransactionDate() == null) {
            settlement.setTransactionDate(LocalDateTime.now());
        }
        settlement.setWorkspaceId(SecurityAuth.requireWorkspaceId());

        IspSettlement saved = ispSettlementRepository.save(settlement);
        return new StandardResponse_SettlementCreated(
                LocalDateTime.now(),
                "CREATED",
                null,
                new SettlementCreatedData(saved.getId())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StandardResponse_DailyCashSummaryData getDailySummary(LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("targetDate is required");
        }

        String workspaceId = SecurityAuth.requireWorkspaceId();
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(LocalTime.MAX);

        List<DailyTransaction> transactions =
                dailyTransactionRepository.findByWorkspaceIdAndRecordedAtBetween(workspaceId, start, end);
        BigDecimal totalCollected = BigDecimal.ZERO;
        for (DailyTransaction transaction : transactions) {
            if (!isCollectionVisible(transaction)) {
                continue;
            }
            totalCollected = totalCollected.add(transaction.getAmountCollected());
        }

        BigDecimal totalExpensed = BigDecimal.ZERO;
        BigDecimal totalSettlements = BigDecimal.ZERO;
        if (SecurityAuth.isOwner()) {
            List<DailyExpense> expenses =
                    dailyExpenseRepository.findByWorkspaceIdAndLoggedAtBetween(workspaceId, start, end);
            for (DailyExpense expense : expenses) {
                totalExpensed = totalExpensed.add(expense.getAmount());
            }

            List<IspSettlement> settlements =
                    ispSettlementRepository.findByWorkspaceIdAndTransactionDateBetween(workspaceId, start, end);
            for (IspSettlement settlement : settlements) {
                totalSettlements = totalSettlements.add(settlement.getAmountPaid());
            }
        }

        BigDecimal netCashInHand = totalCollected.subtract(totalExpensed).subtract(totalSettlements);

        DailyCashSummary summary = new DailyCashSummary(
                totalCollected,
                totalExpensed,
                totalSettlements,
                netCashInHand
        );

        return new StandardResponse_DailyCashSummaryData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                summary
        );
    }

    @Override
    @Transactional(readOnly = true)
    public StandardResponse_DailyLedgerBook getDailyLedgerBook(LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("targetDate is required");
        }

        String workspaceId = SecurityAuth.requireWorkspaceId();
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(LocalTime.MAX);

        List<DailyLedgerTransactionDTO> transactions = new ArrayList<>();

        for (DailyTransaction tx : dailyTransactionRepository.findByWorkspaceIdAndRecordedAtBetween(
                workspaceId, start, end)) {
            if (!isCollectionVisible(tx)) {
                continue;
            }
            Customer customer = tx.getCustomer();
            Employee agent = tx.getFieldAgent();
            transactions.add(new DailyLedgerTransactionDTO(
                    tx.getTransactionId(),
                    customer != null ? customer.getFullName() : "Unknown",
                    customer != null && customer.getBlockName() != null ? customer.getBlockName() : "",
                    tx.getRecordedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    tx.getAmountCollected(),
                    tx.getModeOfPayment() == PaymentMode.ONLINE_UPI ? "ONLINE_UPI" : "CASH",
                    agent != null ? agent.getFullName() : "",
                    false,
                    null,
                    false,
                    null
            ));
        }

        if (SecurityAuth.isOwner()) {
            for (DailyExpense expense : dailyExpenseRepository.findByWorkspaceIdAndLoggedAtBetween(
                    workspaceId, start, end)) {
                transactions.add(new DailyLedgerTransactionDTO(
                        "exp-" + expense.getId(),
                        expense.getDescription(),
                        expense.getExpenseCategory() != null ? expense.getExpenseCategory().name() : "EXPENSE",
                        expense.getLoggedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        expense.getAmount(),
                        "CASH",
                        expense.getLoggedByEmployeeId() != null ? expense.getLoggedByEmployeeId() : "",
                        true,
                        expense.getExpenseCategory() != null ? expense.getExpenseCategory().name() : null,
                        false,
                        null
                ));
            }

            for (IspSettlement settlement : ispSettlementRepository.findByWorkspaceIdAndTransactionDateBetween(
                    workspaceId, start, end)) {
                transactions.add(new DailyLedgerTransactionDTO(
                        "settle-" + settlement.getId(),
                        "Settlement to " + settlement.getConnectionTypeName(),
                        settlement.getConnectionTypeName(),
                        settlement.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        settlement.getAmountPaid(),
                        "CASH",
                        "",
                        false,
                        null,
                        true,
                        settlement.getPaymentStatus()
                ));
            }
        }

        transactions.sort(Comparator.comparing(DailyLedgerTransactionDTO::timestamp).reversed());

        List<DailyTransaction> visibleCollections = dailyTransactionRepository
                .findByWorkspaceIdAndRecordedAtBetween(workspaceId, start, end).stream()
                .filter(this::isCollectionVisible)
                .toList();

        int homesPaid = (int) visibleCollections.stream()
                .map(tx -> tx.getCustomer() != null ? tx.getCustomer().getCustomerId() : null)
                .distinct()
                .count();

        BigDecimal totalCollected = visibleCollections.stream()
                .map(DailyTransaction::getAmountCollected)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DailyLedgerBookData data = new DailyLedgerBookData(
                new DailyLedgerSummaryDTO(totalCollected, homesPaid),
                transactions
        );

        return new StandardResponse_DailyLedgerBook(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                data
        );
    }

    @Override
    @Transactional
    public void recordManualCollection(RecordDailyTransactionRequestDto request, String agentEmployeeId) {
        if (request.amountCollected() == null || request.amountCollected() <= 0) {
            throw new IllegalArgumentException("amount_collected must be a positive number");
        }

        Customer customer = resolveCustomer(request);
        if (customer == null) {
            String lookup = request.customerId() != null && !request.customerId().isBlank()
                    ? request.customerId()
                    : request.customerName();
            throw new IllegalArgumentException("Customer not found: " + lookup);
        }
        workspaceAuthorizationService.assertCustomerAccess(customer.getCustomerId());

        Employee fieldAgent = workspaceAuthorizationService.requireFieldAgentInWorkspace(
                agentEmployeeId, employeeRepository);

        PaymentMode mode = PaymentMode.CASH;
        if (request.paymentType() != null && request.paymentType().toUpperCase().contains("UPI")) {
            mode = PaymentMode.ONLINE_UPI;
        }

        LocalDate paymentDate = LocalDate.now();
        if (request.date() != null && !request.date().isBlank()) {
            try {
                paymentDate = LocalDate.parse(request.date().trim());
            } catch (Exception ignored) {
                // keep today
            }
        }

        String billingMonth = PaymentProcessingService.normalizeMonth(paymentDate.getMonth().name());

        paymentProcessingService.recordPayment(
                customer.getCustomerId(),
                BigDecimal.valueOf(request.amountCollected()),
                List.of(billingMonth),
                paymentDate.getYear(),
                mode,
                fieldAgent
        );
    }

    private Customer resolveCustomer(RecordDailyTransactionRequestDto request) {
        String workspaceId = SecurityAuth.requireWorkspaceId();
        if (request.customerId() != null && !request.customerId().isBlank()) {
            return customerRepository.findByCustomerIdAndWorkspaceId(
                    request.customerId().trim(), workspaceId).orElse(null);
        }
        if (request.customerName() == null || request.customerName().isBlank()) {
            return null;
        }
        List<Customer> matches = customerRepository.findByFullNameContainingIgnoreCaseAndWorkspaceId(
                request.customerName().trim(), workspaceId);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private boolean isCollectionVisible(DailyTransaction transaction) {
        if (SecurityAuth.isOwner()) {
            return true;
        }
        Set<String> allowed = workspaceAuthorizationService.resolveAccessibleTerritoryIds();
        Customer customer = transaction.getCustomer();
        return customer != null
                && customer.getTerritory() != null
                && allowed.contains(customer.getTerritory().getTerritoryId());
    }

    private static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("10000000.00");

    private void validateExpense(DailyExpense expense) {
        if (expense == null) {
            throw new IllegalArgumentException("Expense payload is required");
        }
        if (expense.getAmount() == null || expense.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be a positive number");
        }
        if (expense.getAmount().compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new IllegalArgumentException("Transaction amount exceeds maximum permissible business limit");
        }
        if (expense.getDescription() == null || expense.getDescription().isBlank()) {
            throw new IllegalArgumentException("description is required");
        }
        if (expense.getExpenseCategory() == null) {
            throw new IllegalArgumentException("expenseCategory is required");
        }
    }

    private void validateSettlement(IspSettlement settlement) {
        if (settlement == null) {
            throw new IllegalArgumentException("Settlement payload is required");
        }
        if (settlement.getConnectionTypeName() == null || settlement.getConnectionTypeName().isBlank()) {
            throw new IllegalArgumentException("connectionTypeName is required");
        }
        if (settlement.getAmountPaid() == null || settlement.getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amountPaid must be a positive number");
        }
        if (settlement.getAmountPaid().compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new IllegalArgumentException("Transaction amount exceeds maximum permissible business limit");
        }
        if (settlement.getPaymentStatus() == null || settlement.getPaymentStatus().isBlank()) {
            throw new IllegalArgumentException("paymentStatus is required");
        }

        String paymentStatus = settlement.getPaymentStatus().trim();
        if (!paymentStatus.equals("FULL_PAYMENT") && !paymentStatus.equals("PARTIAL_PAYMENT")) {
            throw new IllegalArgumentException("paymentStatus must be FULL_PAYMENT or PARTIAL_PAYMENT");
        }
        settlement.setPaymentStatus(paymentStatus);
    }
}
