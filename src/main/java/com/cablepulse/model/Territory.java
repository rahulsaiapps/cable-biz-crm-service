package com.cablepulse.model;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "territories")
@SQLDelete(sql = "UPDATE territories SET is_deleted = true WHERE territory_id = ?")
@SQLRestriction("is_deleted = false")
public class Territory {

    @Id
    @Column(name = "territory_id")
    private String territoryId;

    @Column(name = "location_name", nullable = false)
    private String locationName;

    @Column(name = "district")
    private String district;

    @Column(name = "state")
    private String state;

    @Column(name = "workspace_id", nullable = false)
    private String workspaceId;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @OneToMany(mappedBy = "territory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TerritoryBlock> blocks = new ArrayList<>();

    public Territory() {}

    public Territory(String territoryId, String locationName) {
        this.territoryId = territoryId;
        this.locationName = locationName;
    }

    public String getTerritoryId() {
        return territoryId;
    }

    public void setTerritoryId(String territoryId) {
        this.territoryId = territoryId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<TerritoryBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<TerritoryBlock> blocks) {
        this.blocks = blocks;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }
}
