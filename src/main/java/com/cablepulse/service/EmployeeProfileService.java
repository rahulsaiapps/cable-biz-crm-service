package com.cablepulse.service;

import com.cablepulse.dto.DtoClasses.UpdateProfileRequestDto;
import com.cablepulse.exception.EmployeeNotFoundException;
import com.cablepulse.model.Employee;
import com.cablepulse.repository.EmployeeRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeProfileService {

    private final EmployeeRepository employeeRepository;

    public EmployeeProfileService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public Employee updateCurrentEmployeeProfile(UpdateProfileRequestDto requestDto) {
        String firebaseUid = resolveAuthenticatedFirebaseUid();

        Employee employee = employeeRepository.findById(firebaseUid)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + firebaseUid));

        if (requestDto.fullName() != null) {
            String fullName = requestDto.fullName().trim();
            if (fullName.isEmpty()) {
                throw new IllegalArgumentException("fullName cannot be blank");
            }
            employee.setFullName(fullName);
        }

        if (requestDto.email() != null) {
            String email = requestDto.email().trim();
            employee.setEmail(email.isEmpty() ? null : email);
        }

        if (requestDto.description() != null) {
            String description = requestDto.description().trim();
            employee.setDescription(description.isEmpty() ? null : description);
        }

        return employeeRepository.save(employee);
    }

    private String resolveAuthenticatedFirebaseUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new EmployeeNotFoundException("No authenticated employee principal");
        }
        return authentication.getName();
    }
}
