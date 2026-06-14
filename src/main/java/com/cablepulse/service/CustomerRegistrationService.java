package com.cablepulse.service;

import com.cablepulse.dto.CreateCustomerRequestDto;
import com.cablepulse.model.Customer;
import com.cablepulse.model.CustomerLedger;
import com.cablepulse.model.GlobalPlan;
import com.cablepulse.model.LedgerStatus;
import com.cablepulse.model.Territory;
import com.cablepulse.repository.CustomerLedgerRepository;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.GlobalPlanRepository;
import com.cablepulse.repository.TerritoryRepository;
import com.cablepulse.security.SecurityAuth;
import com.cablepulse.security.WorkspaceAuthorizationService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class CustomerRegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerRegistrationService.class);
    private static final int MAX_SERIAL_ALLOCATION_ATTEMPTS = 8;

    private final CustomerRepository customerRepository;
    private final CustomerLedgerRepository customerLedgerRepository;
    private final TerritoryRepository territoryRepository;
    private final GlobalPlanRepository globalPlanRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private CustomerRegistrationService self;

    public CustomerRegistrationService(
            CustomerRepository customerRepository,
            CustomerLedgerRepository customerLedgerRepository,
            TerritoryRepository territoryRepository,
            GlobalPlanRepository globalPlanRepository,
            WorkspaceAuthorizationService workspaceAuthorizationService) {
        this.customerRepository = customerRepository;
        this.customerLedgerRepository = customerLedgerRepository;
        this.territoryRepository = territoryRepository;
        this.globalPlanRepository = globalPlanRepository;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    @Autowired
    @Lazy
    void setSelf(CustomerRegistrationService self) {
        this.self = self;
    }

    public Customer registerCustomer(CreateCustomerRequestDto request) {
        String territoryId = request.getTerritoryId().trim();
        workspaceAuthorizationService.assertTerritoryAccess(territoryId);
        if (!territoryRepository.existsById(territoryId)) {
            throw new EntityNotFoundException("Territory not found: " + territoryId);
        }

        String matchedPlanId = resolvePlanId(request.getPlanName());

        DataIntegrityViolationException lastConflict = null;

        for (int attempt = 0; attempt < MAX_SERIAL_ALLOCATION_ATTEMPTS; attempt++) {
            String customerId = "cust_" + UUID.randomUUID().toString().replace("-", "");
            int serialNumber = customerRepository.allocateNextSerialNumber(territoryId);

            try {
                return self.saveCustomerAttempt(
                        customerId,
                        serialNumber,
                        request,
                        territoryId,
                        matchedPlanId);
            } catch (DataIntegrityViolationException ex) {
                lastConflict = ex;
                logger.warn(
                        "Customer serial allocation conflict territoryId={} serial={} attempt={}/{} cause={}",
                        territoryId,
                        serialNumber,
                        attempt + 1,
                        MAX_SERIAL_ALLOCATION_ATTEMPTS,
                        ex.getMostSpecificCause() != null
                                ? ex.getMostSpecificCause().getMessage()
                                : ex.getMessage());
            }
        }

        throw lastConflict != null
                ? lastConflict
                : new IllegalStateException("Unable to allocate customer serial number");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Customer saveCustomerAttempt(
            String customerId,
            int serialNumber,
            CreateCustomerRequestDto request,
            String territoryId,
            String matchedPlanId) {
        Territory territory = territoryRepository.getReferenceById(territoryId);
        GlobalPlan matchedPlan = matchedPlanId != null
                ? globalPlanRepository.getReferenceById(matchedPlanId)
                : null;
        Customer customer = buildCustomer(
                customerId,
                serialNumber,
                request,
                territory,
                matchedPlan);
        customer.setWorkspaceId(territory.getWorkspaceId());
        Customer saved = customerRepository.save(customer);
        createOpeningLedgerIfBillable(saved, request);
        return saved;
    }

    private void createOpeningLedgerIfBillable(Customer customer, CreateCustomerRequestDto request) {
        BigDecimal rate = resolveMonthlyRate(customer, request);
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        YearMonth billingPeriod = YearMonth.now();
        String billingMonth = PaymentProcessingService.normalizeMonth(
                billingPeriod.getMonth().name());

        CustomerLedger ledger = new CustomerLedger(
                billingMonth,
                billingPeriod.getYear(),
                LedgerStatus.UNPAID,
                BigDecimal.ZERO,
                rate,
                customer);
        customerLedgerRepository.save(ledger);
    }

    private static BigDecimal resolveMonthlyRate(Customer customer, CreateCustomerRequestDto request) {
        if (request.getPlanMonthlyRate() != null
                && request.getPlanMonthlyRate().compareTo(BigDecimal.ZERO) > 0) {
            return request.getPlanMonthlyRate();
        }
        if (customer.getCustomRateOverride() != null
                && customer.getCustomRateOverride().compareTo(BigDecimal.ZERO) > 0) {
            return customer.getCustomRateOverride();
        }
        if (customer.getGlobalPlan() != null
                && customer.getGlobalPlan().getMonthlyRate() != null
                && customer.getGlobalPlan().getMonthlyRate().compareTo(BigDecimal.ZERO) > 0) {
            return customer.getGlobalPlan().getMonthlyRate();
        }
        return null;
    }

    private String resolvePlanId(String planName) {
        if (planName == null || planName.isBlank()) {
            return null;
        }
        return globalPlanRepository.findByWorkspaceId(SecurityAuth.requireWorkspaceId()).stream()
                .filter(plan -> planName.equalsIgnoreCase(plan.getPlanName()))
                .map(GlobalPlan::getPlanId)
                .findFirst()
                .orElse(null);
    }

    private static Customer buildCustomer(
            String customerId,
            int serialNumber,
            CreateCustomerRequestDto request,
            Territory territory,
            GlobalPlan matchedPlan) {
        return new Customer(
                customerId,
                serialNumber,
                request.getName().trim(),
                blankToNull(request.getPhoneNumber()),
                blankToNull(request.getStreet()),
                blankToNull(request.getDoorNumber()),
                request.getPlanMonthlyRate(),
                territory,
                matchedPlan,
                blankToNull(request.getConnectionType()),
                blankToNull(request.getBoxNumber()),
                blankToNull(request.getCardNumber())
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
