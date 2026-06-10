package com.cablepulse.service;

import com.cablepulse.dto.CreateCustomerRequestDto;
import com.cablepulse.model.Customer;
import com.cablepulse.model.GlobalPlan;
import com.cablepulse.model.Territory;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.GlobalPlanRepository;
import com.cablepulse.repository.TerritoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomerRegistrationService {

    private final CustomerRepository customerRepository;
    private final TerritoryRepository territoryRepository;
    private final GlobalPlanRepository globalPlanRepository;

    public CustomerRegistrationService(
            CustomerRepository customerRepository,
            TerritoryRepository territoryRepository,
            GlobalPlanRepository globalPlanRepository) {
        this.customerRepository = customerRepository;
        this.territoryRepository = territoryRepository;
        this.globalPlanRepository = globalPlanRepository;
    }

    @Transactional
    public Customer registerCustomer(CreateCustomerRequestDto request) {
        Territory territory = territoryRepository.findById(request.getTerritoryId().trim())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Territory not found: " + request.getTerritoryId()));

        int serialNumber =
                customerRepository.findMaxSerialNumberByTerritoryId(territory.getTerritoryId()) + 1;
        String customerId = "cust_" + UUID.randomUUID().toString().replace("-", "");

        GlobalPlan matchedPlan = null;
        String planName = request.getPlanName();
        if (planName != null && !planName.isBlank()) {
            matchedPlan = globalPlanRepository.findAll().stream()
                    .filter(plan -> planName.equalsIgnoreCase(plan.getPlanName()))
                    .findFirst()
                    .orElse(null);
        }

        Customer customer = new Customer(
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

        return customerRepository.save(customer);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
