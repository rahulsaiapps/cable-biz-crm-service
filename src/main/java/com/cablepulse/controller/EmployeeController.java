package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.exception.EmployeeNotFoundException;
import com.cablepulse.model.Employee;
import com.cablepulse.repository.EmployeeRepository;
import com.cablepulse.service.EmployeeProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final EmployeeProfileService employeeProfileService;

    public EmployeeController(
            EmployeeRepository employeeRepository,
            EmployeeProfileService employeeProfileService) {
        this.employeeRepository = employeeRepository;
        this.employeeProfileService = employeeProfileService;
    }

    @PostMapping
    public ResponseEntity<StandardResponse_EmployeeData> createEmployee(@Valid @RequestBody CreateEmployeeRequestDto requestDto) {
        String pendingEmployeeId = "PENDING-" + UUID.randomUUID();

        Employee employee = new Employee(pendingEmployeeId, requestDto.fullName(), requestDto.role());
        if (requestDto.email() != null && !requestDto.email().isBlank()) {
            employee.setEmail(requestDto.email().trim());
        }
        Employee saved = employeeRepository.save(employee);

        StandardResponse_EmployeeData response = new StandardResponse_EmployeeData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                new EmployeeDTO(saved.getEmployeeId(), saved.getFullName(), saved.getRole())
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/profile")
    public ResponseEntity<StandardResponse_EmployeeProfileData> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDto requestDto) {
        Employee saved = employeeProfileService.updateCurrentEmployeeProfile(requestDto);

        StandardResponse_EmployeeProfileData response = new StandardResponse_EmployeeProfileData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                toEmployeeProfileDto(saved)
        );

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<StandardResponse_Void> handleEmployeeNotFound(EmployeeNotFoundException ex) {
        StandardResponse_Void response = new StandardResponse_Void(
                LocalDateTime.now(),
                "ERROR",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<StandardResponse_Void> handleInvalidProfileUpdate(IllegalArgumentException ex) {
        StandardResponse_Void response = new StandardResponse_Void(
                LocalDateTime.now(),
                "ERROR",
                ex.getMessage(),
                null
        );
        return ResponseEntity.badRequest().body(response);
    }

    private static EmployeeProfileDTO toEmployeeProfileDto(Employee employee) {
        return new EmployeeProfileDTO(
                employee.getEmployeeId(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getDescription(),
                employee.getRole()
        );
    }

    public record StandardResponse_Void(
            LocalDateTime timestamp,
            String status,
            String error,
            Void data
    ) {}
}
