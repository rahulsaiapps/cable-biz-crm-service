package com.cablepulse.model;

import jakarta.persistence.*;

@Entity
@Table(name = "territory_blocks")
public class TerritoryBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "block_id")
    private Long blockId;

    @Column(name = "block_name", nullable = false)
    private String blockName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "territory_id", nullable = false)
    private Territory territory;

    public TerritoryBlock() {}

    public TerritoryBlock(String blockName, Territory territory) {
        this.blockName = blockName;
        this.territory = territory;
    }

    public Long getBlockId() {
        return blockId;
    }

    public void setBlockId(Long blockId) {
        this.blockId = blockId;
    }

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public Territory getTerritory() {
        return territory;
    }

    public void setTerritory(Territory territory) {
        this.territory = territory;
    }
}
