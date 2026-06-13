package com.cablepulse.testsupport;

import com.cablepulse.model.ConnectionProvider;
import com.cablepulse.model.Employee;
import com.cablepulse.model.EmployeeRole;
import com.cablepulse.model.Territory;
import com.cablepulse.model.Workspace;
import com.cablepulse.repository.WorkspaceRepository;
import org.springframework.stereotype.Component;

/**
 * Seeds a default workspace + owner for integration tests (H2, Flyway disabled).
 */
@Component
public class TestWorkspaceSupport {

    public static final String WORKSPACE_ID = "ws_test";
    public static final String OWNER_UID = "owner-uid";

    private final WorkspaceRepository workspaceRepository;

    public TestWorkspaceSupport(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    public Workspace seedDefaultWorkspace() {
        return workspaceRepository.save(new Workspace(WORKSPACE_ID, "Test Business", OWNER_UID));
    }

    public Employee ownerEmployee() {
        Employee owner = new Employee(OWNER_UID, "Test Owner", EmployeeRole.OWNER);
        owner.setWorkspaceId(WORKSPACE_ID);
        return owner;
    }

    public Territory territory(String territoryId, String locationName) {
        Territory territory = new Territory(territoryId, locationName);
        territory.setWorkspaceId(WORKSPACE_ID);
        return territory;
    }

    public ConnectionProvider provider(String name) {
        return new ConnectionProvider(WORKSPACE_ID, name);
    }
}
