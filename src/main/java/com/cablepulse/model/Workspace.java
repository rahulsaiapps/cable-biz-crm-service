package com.cablepulse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "workspaces")
public class Workspace {

    @Id
    @Column(name = "workspace_id")
    private String workspaceId;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "created_by_uid")
    private String createdByUid;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Workspace() {}

    public Workspace(String workspaceId, String businessName, String createdByUid) {
        this.workspaceId = workspaceId;
        this.businessName = businessName;
        this.createdByUid = createdByUid;
        this.createdAt = LocalDateTime.now();
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getCreatedByUid() {
        return createdByUid;
    }

    public void setCreatedByUid(String createdByUid) {
        this.createdByUid = createdByUid;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
