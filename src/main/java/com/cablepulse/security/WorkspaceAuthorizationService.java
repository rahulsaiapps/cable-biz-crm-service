package com.cablepulse.security;

import com.cablepulse.model.Customer;
import com.cablepulse.model.Employee;
import com.cablepulse.model.Territory;
import com.cablepulse.repository.CustomerRepository;
import com.cablepulse.repository.EmployeeRepository;
import com.cablepulse.repository.TerritoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enforces workspace territory and customer access for collection agents.
 * Owners may access all territories; collection agents are limited to assigned villages.
 */
@Service
public class WorkspaceAuthorizationService {

    private final EmployeeRepository employeeRepository;
    private final TerritoryRepository territoryRepository;
    private final CustomerRepository customerRepository;

    public WorkspaceAuthorizationService(
            EmployeeRepository employeeRepository,
            TerritoryRepository territoryRepository,
            CustomerRepository customerRepository) {
        this.employeeRepository = employeeRepository;
        this.territoryRepository = territoryRepository;
        this.customerRepository = customerRepository;
    }

    public boolean canViewSensitiveCustomerFields() {
        return SecurityAuth.isOwner();
    }

    public void assertTerritoryAccess(String territoryId) {
        if (SecurityAuth.isOwner()) {
            return;
        }
        if (territoryId == null || territoryId.isBlank()) {
            throw new AccessDeniedException("Territory access denied");
        }
        Set<String> allowed = resolveAccessibleTerritoryIds();
        if (!allowed.contains(territoryId.trim())) {
            throw new AccessDeniedException("Territory access denied");
        }
    }

    public void assertCustomerAccess(String customerId) {
        Customer customer = customerRepository.findById(customerId.trim())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));
        String territoryId = customer.getTerritory() != null
                ? customer.getTerritory().getTerritoryId()
                : null;
        if (territoryId == null || territoryId.isBlank()) {
            throw new AccessDeniedException("Customer access denied");
        }
        assertTerritoryAccess(territoryId);
    }

    public Set<String> resolveAccessibleTerritoryIds() {
        if (SecurityAuth.isOwner()) {
            return territoryRepository.findAll().stream()
                    .map(Territory::getTerritoryId)
                    .collect(Collectors.toSet());
        }

        String userId = SecurityAuth.currentUserId();
        if (userId == null || userId.isBlank()) {
            return Set.of();
        }

        Optional<Employee> employee = employeeRepository.findById(userId);
        if (employee.isEmpty()) {
            return Set.of();
        }

        List<String> assignedVillages = employee.get().getAssignedVillages();
        if (assignedVillages == null || assignedVillages.isEmpty()) {
            return Set.of();
        }

        Set<String> normalizedVillages = assignedVillages.stream()
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<String> territoryIds = new HashSet<>();
        for (Territory territory : territoryRepository.findAll()) {
            String location = territory.getLocationName();
            if (location != null && normalizedVillages.contains(location.trim().toLowerCase())) {
                territoryIds.add(territory.getTerritoryId());
            }
        }
        return territoryIds;
    }

    public List<Territory> filterAccessibleTerritories(List<Territory> territories) {
        if (SecurityAuth.isOwner()) {
            return territories;
        }
        Set<String> allowed = resolveAccessibleTerritoryIds();
        return territories.stream()
                .filter(t -> allowed.contains(t.getTerritoryId()))
                .toList();
    }

    public List<Customer> filterAccessibleCustomers(List<Customer> customers) {
        if (SecurityAuth.isOwner()) {
            return customers;
        }
        Set<String> allowed = resolveAccessibleTerritoryIds();
        return customers.stream()
                .filter(c -> c.getTerritory() != null
                        && allowed.contains(c.getTerritory().getTerritoryId()))
                .toList();
    }
}
