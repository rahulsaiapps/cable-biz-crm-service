package com.cablepulse.service;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.model.*;
import com.cablepulse.repository.CustomerLedgerRepository;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.EmployeeRepository;
import com.cablepulse.repository.GlobalPlanRepository;
import com.cablepulse.security.SecurityAuth;
import com.cablepulse.security.WorkspaceAuthorizationService;
import com.cablepulse.util.PiiMaskingUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerLedgerRepository customerLedgerRepository;
    private final GlobalPlanRepository globalPlanRepository;
    private final EmployeeRepository employeeRepository;
    private final PaymentProcessingService paymentProcessingService;
    private final CustomerBalanceService customerBalanceService;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    public CustomerService(
            CustomerRepository customerRepository,
            CustomerLedgerRepository customerLedgerRepository,
            GlobalPlanRepository globalPlanRepository,
            EmployeeRepository employeeRepository,
            PaymentProcessingService paymentProcessingService,
            CustomerBalanceService customerBalanceService,
            WorkspaceAuthorizationService workspaceAuthorizationService) {
        this.customerRepository = customerRepository;
        this.customerLedgerRepository = customerLedgerRepository;
        this.globalPlanRepository = globalPlanRepository;
        this.employeeRepository = employeeRepository;
        this.paymentProcessingService = paymentProcessingService;
        this.customerBalanceService = customerBalanceService;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    @Transactional(readOnly = true)
    public CustomerProfileDTO getCustomerProfile(String customerId) {
        workspaceAuthorizationService.assertCustomerAccess(customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));
        return toProfileDto(customer);
    }

    @Transactional
    public void softDeleteCustomer(String customerId) {
        workspaceAuthorizationService.assertCustomerAccess(customerId);
        Customer customer = customerRepository.findById(customerId.trim())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));
        customer.setDeleted(true);
        customerRepository.save(customer);
    }

    @Transactional
    public void collectPayment(String customerId, CollectPaymentRequestDto request, String agentEmployeeId) {
        workspaceAuthorizationService.assertCustomerAccess(customerId);
        Employee fieldAgent = resolveFieldAgent(agentEmployeeId);
        int year = request.year() != null ? request.year() : java.time.LocalDate.now().getYear();
        List<String> months = request.resolvedMonths();

        PaymentMode mode = PaymentMode.CASH;
        if (request.paymentMode() != null && !request.paymentMode().isBlank()) {
            try {
                mode = PaymentMode.valueOf(request.paymentMode().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                if ("UPI".equalsIgnoreCase(request.paymentMode())) {
                    mode = PaymentMode.ONLINE_UPI;
                }
            }
        }

        paymentProcessingService.recordPayment(
                customerId,
                BigDecimal.valueOf(request.amount()),
                months,
                year,
                mode,
                fieldAgent
        );
    }

    @Transactional
    public CustomerProfileDTO updateSubscription(String customerId, UpdateSubscriptionRequestDto request) {
        workspaceAuthorizationService.assertCustomerAccess(customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));

        String planName = request.resolvedPlanName();
        int monthlyRate = request.resolvedMonthlyRate();

        GlobalPlan matchedPlan = globalPlanRepository.findByWorkspaceId(SecurityAuth.requireWorkspaceId()).stream()
                .filter(plan -> planName.equalsIgnoreCase(plan.getPlanName()))
                .findFirst()
                .orElse(null);

        if (matchedPlan == null) {
            matchedPlan = new GlobalPlan(
                    "plan-" + java.util.UUID.randomUUID(),
                    planName,
                    BigDecimal.valueOf(monthlyRate),
                    null
            );
            matchedPlan.setWorkspaceId(SecurityAuth.requireWorkspaceId());
            matchedPlan = globalPlanRepository.save(matchedPlan);
        }

        customer.setCustomRateOverride(BigDecimal.valueOf(monthlyRate));
        customer.setGlobalPlan(matchedPlan);
        Customer saved = customerRepository.save(customer);
        return toProfileDto(saved);
    }

    private Employee resolveFieldAgent(String agentEmployeeId) {
        return workspaceAuthorizationService.requireFieldAgentInWorkspace(agentEmployeeId, employeeRepository);
    }

    private CustomerProfileDTO toProfileDto(Customer customer) {
        BigDecimal balanceDue = customerBalanceService.sumTotalDueForCustomer(customer.getCustomerId());

        String planName = customer.getGlobalPlan() != null
                ? customer.getGlobalPlan().getPlanName()
                : "Custom Plan";
        BigDecimal monthlyRate = customer.getCustomRateOverride() != null
                ? customer.getCustomRateOverride()
                : (customer.getGlobalPlan() != null
                        ? customer.getGlobalPlan().getMonthlyRate()
                        : BigDecimal.ZERO);

        String territoryId = customer.getTerritory() != null
                ? customer.getTerritory().getTerritoryId()
                : "";
        String territoryName = customer.getTerritory() != null
                ? customer.getTerritory().getLocationName()
                : "";

        String paymentStatus;
        if (!customerLedgerRepository.existsByCustomer_CustomerId(customer.getCustomerId())) {
            paymentStatus = "UNPAID";
            if (monthlyRate.compareTo(BigDecimal.ZERO) > 0) {
                balanceDue = monthlyRate;
            }
        } else {
            paymentStatus = CustomerBalanceService.paymentStatusFromBalance(balanceDue);
        }

        String mobileNumber = customer.getMobileNumber();
        if (!workspaceAuthorizationService.canViewSensitiveCustomerFields()) {
            mobileNumber = PiiMaskingUtil.maskPhone(mobileNumber);
        }

        return new CustomerProfileDTO(
                customer.getCustomerId(),
                customer.getFullName(),
                customer.getDoorNumber(),
                customer.getBlockName(),
                territoryId,
                territoryName,
                planName,
                monthlyRate,
                paymentStatus,
                balanceDue,
                mobileNumber
        );
    }
}
