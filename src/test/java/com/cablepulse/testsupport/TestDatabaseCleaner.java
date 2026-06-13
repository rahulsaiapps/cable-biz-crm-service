package com.cablepulse.testsupport;

import com.cablepulse.repository.EmployeeRepository;
import com.cablepulse.repository.WorkspaceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hard-deletes rows for integration tests. Required because {@code deleteAll()} on
 * {@link com.cablepulse.model.Customer} performs a soft delete and leaves FK references.
 */
@Component
public class TestDatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;

    public TestDatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void wipeAndSeedDefaultWorkspace(
            WorkspaceRepository workspaceRepository,
            EmployeeRepository employeeRepository,
            TestWorkspaceSupport workspaceSupport) {
        wipeCoreWorkspaceData();
        workspaceSupport.seedDefaultWorkspace();
        employeeRepository.save(workspaceSupport.ownerEmployee());
    }

    public void wipeCoreWorkspaceData() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            jdbcTemplate.update("DELETE FROM customer_ledgers");
            jdbcTemplate.update("DELETE FROM daily_transactions");
            jdbcTemplate.update("DELETE FROM daily_expenses");
            jdbcTemplate.update("DELETE FROM isp_vendor_settlements");
            jdbcTemplate.update("DELETE FROM customers");
            jdbcTemplate.update("DELETE FROM global_plans");
            jdbcTemplate.update("DELETE FROM territory_blocks");
            jdbcTemplate.update("DELETE FROM territories");
            jdbcTemplate.update("DELETE FROM connection_providers");
            jdbcTemplate.update("DELETE FROM employee_assigned_villages");
            jdbcTemplate.update("DELETE FROM employees");
            jdbcTemplate.update("DELETE FROM workspaces");
        } finally {
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }
}
