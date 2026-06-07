package com.cablepulse.service;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.model.DailyExpense;
import com.cablepulse.model.DailyTransaction;
import com.cablepulse.model.IspSettlement;
import com.cablepulse.repository.DailyExpenseRepository;
import com.cablepulse.repository.DailyTransactionRepository;
import com.cablepulse.repository.IspSettlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class DailyLedgerServiceImpl implements DailyLedgerService {

    private final DailyExpenseRepository dailyExpenseRepository;
    private final DailyTransactionRepository dailyTransactionRepository;
    private final IspSettlementRepository ispSettlementRepository;

    public DailyLedgerServiceImpl(
            DailyExpenseRepository dailyExpenseRepository,
            DailyTransactionRepository dailyTransactionRepository,
            IspSettlementRepository ispSettlementRepository) {
        this.dailyExpenseRepository = dailyExpenseRepository;
        this.dailyTransactionRepository = dailyTransactionRepository;
        this.ispSettlementRepository = ispSettlementRepository;
    }

    @Override
    @Transactional
    public StandardResponse_ExpenseCreated saveExpense(DailyExpense expense) {
        validateExpense(expense);
        expense.setId(null);
        if (expense.getLoggedAt() == null) {
            expense.setLoggedAt(LocalDateTime.now());
        }

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

        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(LocalTime.MAX);

        List<DailyTransaction> transactions = dailyTransactionRepository.findByRecordedAtBetween(start, end);
        double totalCollected = transactions.stream()
                .mapToDouble(t -> t.getAmountCollected().doubleValue())
                .sum();

        List<DailyExpense> expenses = dailyExpenseRepository.findByLoggedAtBetween(start, end);
        double totalExpensed = expenses.stream()
                .mapToDouble(DailyExpense::getAmount)
                .sum();

        List<IspSettlement> settlements = ispSettlementRepository.findByTransactionDateBetween(start, end);
        double totalSettlements = settlements.stream()
                .mapToDouble(IspSettlement::getAmountPaid)
                .sum();

        double netCash = totalCollected - totalExpensed - totalSettlements;

        DailyCashSummary summary = new DailyCashSummary(
                totalCollected,
                totalExpensed,
                totalSettlements,
                netCash
        );

        return new StandardResponse_DailyCashSummaryData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                summary
        );
    }

    private void validateExpense(DailyExpense expense) {
        if (expense == null) {
            throw new IllegalArgumentException("Expense payload is required");
        }
        if (expense.getAmount() == null || expense.getAmount() <= 0) {
            throw new IllegalArgumentException("amount must be a positive number");
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
        if (settlement.getAmountPaid() == null || settlement.getAmountPaid() <= 0) {
            throw new IllegalArgumentException("amountPaid must be a positive number");
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
