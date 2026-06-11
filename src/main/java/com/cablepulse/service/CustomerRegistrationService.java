package com.cablepulse.service;

import com.cablepulse.dto.CreateCustomerRequestDto;
import com.cablepulse.model.Customer;
import com.cablepulse.model.GlobalPlan;
import com.cablepulse.model.Territory;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.GlobalPlanRepository;
import com.cablepulse.repository.TerritoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerRegistrationService {

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
        Territory territory = territoryRepository.findById(request.getTerritoryId().trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Territory not found: " + request.getTerritoryId()));

        GlobalPlan matchedPlan = resolvePlan(request.getPlanName());
        String territoryId = territory.getTerritoryId();

        DataIntegrityViolationException lastConflict = null;

        for (int attempt = 0; attempt < MAX_SERIAL_ALLOCATION_ATTEMPTS; attempt++) {
            String customerId = "cust_" + UUID.randomUUID().toString().replace("-", "");
            int serialNumber = nextAvailableSerial(territoryId) + 1 + attempt;

            try {
                return self.saveCustomerAttempt(
                        customerId,
                        serialNumber,
                        request,
                        territory,
                        matchedPlan);
            } catch (DataIntegrityViolationException ex) {
                lastConflict = ex;
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
            Territory territory,
            GlobalPlan matchedPlan) {
        Customer customer = buildCustomer(
                customerId,
                serialNumber,
                request,
                territory,
                matchedPlan);
        return customerRepository.save(customer);
    }

    private int nextAvailableSerial(String territoryId) {
        int territoryMax = customerRepository.findMaxSerialNumberNative(territoryId);
        int globalMax = customerRepository.findMaxSerialNumberGlobal();
        return Math.max(territoryMax, globalMax);
    }

    private GlobalPlan resolvePlan(String planName) {
        if (planName == null || planName.isBlank()) {
            return null;
        }
        return globalPlanRepository.findAll().stream()
                .filter(plan -> planName.equalsIgnoreCase(plan.getPlanName()))
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
