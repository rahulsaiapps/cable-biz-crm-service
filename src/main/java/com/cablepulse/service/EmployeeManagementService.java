package com.cablepulse.service;

import com.cablepulse.dto.DtoClasses.CreateEmployeeRequestDto;
import com.cablepulse.dto.DtoClasses.EmployeeActivityEntryDTO;
import com.cablepulse.dto.DtoClasses.EmployeeDTO;
import com.cablepulse.exception.EmployeeNotFoundException;
import com.cablepulse.model.DailyExpense;
import com.cablepulse.model.DailyTransaction;
import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.repository.DailyExpenseRepository;
import com.cablepulse.repository.DailyTransactionRepository;
import com.cablepulse.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class EmployeeManagementService {

    private final EmployeeRepository employeeRepository;
    private final DailyTransactionRepository dailyTransactionRepository;
    private final DailyExpenseRepository dailyExpenseRepository;

    public EmployeeManagementService(
            EmployeeRepository employeeRepository,
            DailyTransactionRepository dailyTransactionRepository,
            DailyExpenseRepository dailyExpenseRepository) {
        this.employeeRepository = employeeRepository;
        this.dailyTransactionRepository = dailyTransactionRepository;
        this.dailyExpenseRepository = dailyExpenseRepository;
    }

    @Transactional
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

    @Transactional(readOnly = true)
    public EmployeeDTO getEmployeeDetail(String employeeId) {
        Employee employee = requireEmployee(employeeId);
        return toEmployeeDto(employee);
    }

    @Transactional(readOnly = true)
    public List<EmployeeActivityEntryDTO> getEmployeeActivity(String employeeId) {
        requireEmployee(employeeId);

        LocalDateTime since = LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime until = LocalDate.now().atTime(LocalTime.MAX);

        List<EmployeeActivityEntryDTO> entries = new ArrayList<>();

        for (DailyTransaction tx : dailyTransactionRepository
                .findByFieldAgent_EmployeeIdAndRecordedAtBetweenOrderByRecordedAtDesc(
                        employeeId, since, until)) {
            String customerName = tx.getCustomer() != null ? tx.getCustomer().getFullName() : "Customer";
            String block = tx.getCustomer() != null && tx.getCustomer().getBlockName() != null
                    ? tx.getCustomer().getBlockName()
                    : "";
            entries.add(new EmployeeActivityEntryDTO(
                    "COLLECTION",
                    "Collected from " + customerName,
                    block,
                    tx.getAmountCollected(),
                    tx.getRecordedAt()
            ));
        }

        for (DailyExpense expense : dailyExpenseRepository
                .findByLoggedByEmployeeIdAndLoggedAtBetweenOrderByLoggedAtDesc(
                        employeeId, since, until)) {
            entries.add(new EmployeeActivityEntryDTO(
                    "EXPENSE",
                    expense.getDescription() != null ? expense.getDescription() : "Field expense",
                    expense.getExpenseCategory() != null ? expense.getExpenseCategory().name() : "",
                    expense.getAmount(),
                    expense.getLoggedAt()
            ));
        }

        entries.sort(Comparator.comparing(EmployeeActivityEntryDTO::recordedAt).reversed());
        return entries.size() > 50 ? entries.subList(0, 50) : entries;
    }

    @Transactional
    public void deleteEmployee(String employeeId) {
        Employee employee = requireEmployee(employeeId);
        if (employee.getRole() == EmployeeRole.OWNER) {
            throw new IllegalArgumentException("Owner accounts cannot be removed from the team.");
        }
        employeeRepository.delete(employee);
    }

    public EmployeeDTO toEmployeeDto(Employee employee) {
        long todayCollection = sumTodayCollection(employee.getEmployeeId());
        return new EmployeeDTO(
                employee.getEmployeeId(),
                employee.getFullName(),
                employee.getRole(),
                employee.getEmail(),
                employee.getAssignedVillages() != null
                        ? List.copyOf(employee.getAssignedVillages())
                        : List.of(),
                todayCollection,
                null
        );
    }

    private long sumTodayCollection(String employeeId) {
        LocalDate today = LocalDate.now();
        BigDecimal total = dailyTransactionRepository.sumAmountCollectedByAgentBetween(
                employeeId,
                today.atStartOfDay(),
                today.atTime(LocalTime.MAX));
        return total != null ? total.longValue() : 0L;
    }

    private Employee requireEmployee(String employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Team member not found: " + employeeId));
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
