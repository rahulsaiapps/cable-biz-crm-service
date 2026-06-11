package com.cablepulse.service;

import com.cablepulse.dto.DtoClasses.CreateEmployeeRequestDto;
import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EmployeeManagementService {

    private final EmployeeRepository employeeRepository;

    public EmployeeManagementService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Employee createEmployee(CreateEmployeeRequestDto requestDto) {
        validateCreatableRole(requestDto.role());

        String pendingEmployeeId = "PENDING-" + UUID.randomUUID();
        Employee employee = new Employee(pendingEmployeeId, requestDto.fullName(), requestDto.role());

        if (requestDto.email() != null && !requestDto.email().isBlank()) {
            employee.setEmail(requestDto.email().trim());
        }
        if (requestDto.assignedVillages() != null && !requestDto.assignedVillages().isEmpty()) {
            employee.setAssignedVillages(
                    requestDto.assignedVillages().stream()
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList()
            );
        }
        return employeeRepository.save(employee);
    }

    private static void validateCreatableRole(EmployeeRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Employee role is required");
        }
        if (role == EmployeeRole.OWNER) {
            throw new IllegalArgumentException("OWNER employees cannot be created via API");
        }
    }
}
