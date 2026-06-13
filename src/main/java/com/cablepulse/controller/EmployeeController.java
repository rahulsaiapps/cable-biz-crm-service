package com.cablepulse.controller;

import com.cablepulse.dto.DtoClasses.*;
import com.cablepulse.exception.EmployeeNotFoundException;
import com.cablepulse.model.Employee;
import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.repository.EmployeeRepository;
import com.cablepulse.service.EmployeeManagementService;
import com.cablepulse.service.EmployeeProfileService;
import com.cablepulse.security.SecurityAuth;
import com.cablepulse.util.EtagSupport;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final EmployeeProfileService employeeProfileService;
    private final EmployeeManagementService employeeManagementService;

    public EmployeeController(
            EmployeeRepository employeeRepository,
            EmployeeProfileService employeeProfileService,
            EmployeeManagementService employeeManagementService) {
        this.employeeRepository = employeeRepository;
        this.employeeProfileService = employeeProfileService;
        this.employeeManagementService = employeeManagementService;
    }

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StandardResponse_EmployeeList> listEmployees(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        List<EmployeeDTO> employees = employeeRepository
                .findByWorkspaceId(SecurityAuth.requireWorkspaceId()).stream()
                .map(employeeManagementService::toEmployeeDto)
                .collect(Collectors.toList());

        return EtagSupport.respondWithEtag(ifNoneMatch, employees, () -> {
            StandardResponse_EmployeeList response = new StandardResponse_EmployeeList(
                    LocalDateTime.now(),
                    "SUCCESS",
                    null,
                    employees
            );
            return ResponseEntity.ok(response);
        });
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StandardResponse_EmployeeData> createEmployee(@Valid @RequestBody CreateEmployeeRequestDto requestDto) {
        Employee saved = employeeManagementService.createEmployee(requestDto);

        StandardResponse_EmployeeData response = new StandardResponse_EmployeeData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                employeeManagementService.toEmployeeDto(saved)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StandardResponse_EmployeeData> getEmployee(@PathVariable String employeeId) {
        EmployeeDTO employee = employeeManagementService.getEmployeeDetail(employeeId);
        StandardResponse_EmployeeData response = new StandardResponse_EmployeeData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                employee
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{employeeId}/activity")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StandardResponse_EmployeeActivityList> getEmployeeActivity(
            @PathVariable String employeeId) {
        List<EmployeeActivityEntryDTO> activity =
                employeeManagementService.getEmployeeActivity(employeeId);
        StandardResponse_EmployeeActivityList response = new StandardResponse_EmployeeActivityList(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                activity
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{employeeId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StandardResponse_EmployeeData> updateEmployee(
            @PathVariable String employeeId,
            @Valid @RequestBody UpdateEmployeeRequestDto requestDto) {
        Employee saved = employeeManagementService.updateEmployee(employeeId, requestDto);

        StandardResponse_EmployeeData response = new StandardResponse_EmployeeData(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                employeeManagementService.toEmployeeDto(saved)
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{employeeId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<StandardResponse_Void> deleteEmployee(@PathVariable String employeeId) {
        employeeManagementService.deleteEmployee(employeeId);
        StandardResponse_Void response = new StandardResponse_Void(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                null
        );
        return ResponseEntity.ok(response);
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

    @DeleteMapping("/profile")
    public ResponseEntity<StandardResponse_Void> deleteCurrentAccount() {
        String employeeId = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found for current session"));

        if (employee.getRole() == EmployeeRole.OWNER) {
            throw new IllegalArgumentException(
                    "Owner accounts cannot delete themselves. Remove team members from Team settings instead.");
        }

        employeeRepository.delete(employee);

        StandardResponse_Void response = new StandardResponse_Void(
                LocalDateTime.now(),
                "SUCCESS",
                null,
                null
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
