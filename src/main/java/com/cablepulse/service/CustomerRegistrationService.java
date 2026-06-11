package com.cablepulse.service;

import com.cablepulse.dto.CreateCustomerRequestDto;
import com.cablepulse.model.Customer;
import com.cablepulse.model.GlobalPlan;
import com.cablepulse.model.Territory;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.GlobalPlanRepository;
import com.cablepulse.repository.TerritoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerRegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerRegistrationService.class);
    private static final int MAX_SERIAL_ALLOCATION_ATTEMPTS = 8;

    private final CustomerRepository customerRepository;
    private final TerritoryRepository territoryRepository;
    private final GlobalPlanRepository globalPlanRepository;
    private CustomerRegistrationService self;

    public CustomerRegistrationService(
            CustomerRepository customerRepository,
            TerritoryRepository territoryRepository,
            GlobalPlanRepository globalPlanRepository) {
        this.customerRepository = customerRepository;
        this.territoryRepository = territoryRepository;
        this.globalPlanRepository = globalPlanRepository;
    }

    @Autowired
    @Lazy
    void setSelf(CustomerRegistrationService self) {
        this.self = self;
    }

    public Customer registerCustomer(CreateCustomerRequestDto request) {
        String territoryId = request.getTerritoryId().trim();
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
        return customerRepository.save(customer);
    }

    private String resolvePlanId(String planName) {
        if (planName == null || planName.isBlank()) {
            return null;
        }
        return globalPlanRepository.findAll().stream()
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
