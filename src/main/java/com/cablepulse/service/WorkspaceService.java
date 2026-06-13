package com.cablepulse.service;

import com.cablepulse.model.Workspace;
import com.cablepulse.repository.WorkspaceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkspaceService {

    public static final String LEGACY_WORKSPACE_ID = "ws_legacy_default";

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceService(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional
    public Workspace provisionForNewOwner(String firebaseUid, String defaultBusinessName) {
        String workspaceId = "ws_" + UUID.randomUUID().toString().replace("-", "");
        String businessName = (defaultBusinessName != null && !defaultBusinessName.isBlank())
                ? defaultBusinessName.trim()
                : "My Business";
        Workspace workspace = new Workspace(workspaceId, businessName, firebaseUid);
        return workspaceRepository.save(workspace);
    }

    @Transactional(readOnly = true)
    public Workspace requireWorkspace(String workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("Workspace not found: " + workspaceId));
    }

    @Transactional(readOnly = true)
    public String businessNameFor(String workspaceId) {
        return requireWorkspace(workspaceId).getBusinessName();
    }

    @Transactional
    public Workspace updateBusinessName(String workspaceId, String businessName) {
        if (businessName == null || businessName.isBlank()) {
            throw new IllegalArgumentException("Business name is required");
        }
        Workspace workspace = requireWorkspace(workspaceId);
        workspace.setBusinessName(businessName.trim());
        return workspaceRepository.save(workspace);
    }
}
