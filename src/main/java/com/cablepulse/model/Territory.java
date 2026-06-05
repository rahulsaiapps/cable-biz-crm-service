package com.cablepulse.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "territories")
public class Territory {

    @Id
    @Column(name = "territory_id")
    private String territoryId;

    @Column(name = "location_name", nullable = false)
    private String locationName;

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

    public List<TerritoryBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<TerritoryBlock> blocks) {
        this.blocks = blocks;
    }
}
