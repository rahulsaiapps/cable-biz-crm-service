package com.cablepulse.service;

import com.cablepulse.model.*;
import com.cablepulse.repository.CustomerLedgerRepository;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.DailyTransactionRepository;
import com.cablepulse.security.WorkspaceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentProcessingService {

    private final CustomerRepository customerRepository;
    private final CustomerLedgerRepository customerLedgerRepository;
    private final DailyTransactionRepository dailyTransactionRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    public PaymentProcessingService(
            CustomerRepository customerRepository,
            CustomerLedgerRepository customerLedgerRepository,
            DailyTransactionRepository dailyTransactionRepository,
            WorkspaceAuthorizationService workspaceAuthorizationService) {
        this.customerRepository = customerRepository;
        this.customerLedgerRepository = customerLedgerRepository;
        this.dailyTransactionRepository = dailyTransactionRepository;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    @Transactional
    public void recordPayment(
            String customerId,
            BigDecimal amountCollected,
            List<String> months,
            int year,
            PaymentMode paymentMode,
            Employee fieldAgent) {

        if (amountCollected == null || amountCollected.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be a positive number");
        }
        if (months == null || months.isEmpty()) {
            throw new IllegalArgumentException("At least one billing month is required");
        }

        workspaceAuthorizationService.assertCustomerAccess(customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));

        if (fieldAgent == null) {
            throw new IllegalArgumentException("fieldAgent is required");
        }

        BigDecimal amountPerMonth = amountCollected.divide(
                BigDecimal.valueOf(months.size()), 2, RoundingMode.HALF_UP);

        List<CustomerLedger> ledgers = customerLedgerRepository.findByCustomer_CustomerId(customerId);

        for (String rawMonth : months) {
            String month = normalizeMonth(rawMonth);
            Optional<CustomerLedger> existingLedger = ledgers.stream()
                    .filter(l -> monthMatches(l.getBillingMonth(), month) && l.getBillingYear() == year)
                    .findFirst();

            if (existingLedger.isPresent()) {
                applyPaymentToLedger(existingLedger.get(), amountPerMonth, customer);
                customerLedgerRepository.save(existingLedger.get());
            } else {
                BigDecimal monthlyRate = resolveMonthlyRate(customer);
                if (monthlyRate.compareTo(BigDecimal.ZERO) <= 0) {
                    monthlyRate = amountPerMonth;
                }
                BigDecimal newPaid = amountPerMonth.min(monthlyRate);
                BigDecimal newDue = monthlyRate.subtract(newPaid).max(BigDecimal.ZERO);
                CustomerLedger ledger = new CustomerLedger(
                        month,
                        year,
                        resolveLedgerStatus(newPaid, newDue),
                        newPaid,
                        newDue,
                        customer
                );
                customerLedgerRepository.save(ledger);
            }
        }

        DailyTransaction transaction = new DailyTransaction(
                UUID.randomUUID().toString(),
                amountCollected,
                paymentMode != null ? paymentMode : PaymentMode.CASH,
                LocalDateTime.now(),
                customer,
                fieldAgent
        );
        dailyTransactionRepository.save(transaction);
    }

    private void applyPaymentToLedger(
            CustomerLedger ledger,
            BigDecimal amountApplied,
            Customer customer) {
        BigDecimal monthlyExpected = ledger.getPaidAmount().add(ledger.getDueAmount());
        if (monthlyExpected.compareTo(BigDecimal.ZERO) <= 0) {
            monthlyExpected = resolveMonthlyRate(customer);
        }
        if (monthlyExpected.compareTo(BigDecimal.ZERO) <= 0) {
            monthlyExpected = amountApplied;
        }

        BigDecimal newPaid = ledger.getPaidAmount().add(amountApplied);
        if (newPaid.compareTo(monthlyExpected) > 0) {
            newPaid = monthlyExpected;
        }
        BigDecimal newDue = monthlyExpected.subtract(newPaid).max(BigDecimal.ZERO);

        ledger.setPaidAmount(newPaid);
        ledger.setDueAmount(newDue);
        ledger.setStatus(resolveLedgerStatus(newPaid, newDue));
    }

    private static LedgerStatus resolveLedgerStatus(BigDecimal paidAmount, BigDecimal dueAmount) {
        if (dueAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return LedgerStatus.PAID;
        }
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            return LedgerStatus.PARTIAL;
        }
        return LedgerStatus.UNPAID;
    }

    static BigDecimal resolveMonthlyRate(Customer customer) {
        if (customer.getCustomRateOverride() != null
                && customer.getCustomRateOverride().compareTo(BigDecimal.ZERO) > 0) {
            return customer.getCustomRateOverride();
        }
        if (customer.getGlobalPlan() != null
                && customer.getGlobalPlan().getMonthlyRate() != null
                && customer.getGlobalPlan().getMonthlyRate().compareTo(BigDecimal.ZERO) > 0) {
            return customer.getGlobalPlan().getMonthlyRate();
        }
        return BigDecimal.ZERO;
    }

    public static String normalizeMonth(String month) {
        if (month == null || month.isBlank()) {
            return "JAN";
        }
        String upper = month.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "JANUARY" -> "JAN";
            case "FEBRUARY" -> "FEB";
            case "MARCH" -> "MAR";
            case "APRIL" -> "APR";
            case "MAY" -> "MAY";
            case "JUNE" -> "JUN";
            case "JULY" -> "JUL";
            case "AUGUST" -> "AUG";
            case "SEPTEMBER" -> "SEP";
            case "OCTOBER" -> "OCT";
            case "NOVEMBER" -> "NOV";
            case "DECEMBER" -> "DEC";
            default -> upper.length() > 3 ? upper.substring(0, 3) : upper;
        };
    }

    private static boolean monthMatches(String stored, String normalized) {
        return normalizeMonth(stored).equals(normalizeMonth(normalized));
    }
}
