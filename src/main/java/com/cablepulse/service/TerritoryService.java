package com.cablepulse.service;

import com.cablepulse.dto.TerritoryRequestDto;
import com.cablepulse.exception.TerritoryAlreadyExistsException;
import com.cablepulse.model.Territory;
import com.cablepulse.model.TerritoryBlock;
import com.cablepulse.repository.TerritoryRepository;
import com.cablepulse.security.SecurityAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TerritoryService {

    private final TerritoryRepository territoryRepository;

    public TerritoryService(TerritoryRepository territoryRepository) {
        this.territoryRepository = territoryRepository;
    }

    @Transactional
    public Territory createTerritory(TerritoryRequestDto requestDto) {
        String workspaceId = SecurityAuth.requireWorkspaceId();
        String locationName = requestDto.getLocationName().trim();

        territoryRepository.findByWorkspaceIdAndLocationNameIgnoreCase(workspaceId, locationName)
                .ifPresent(existing -> {
                    throw new TerritoryAlreadyExistsException(existing);
                });

        String territoryId = "ter_" + UUID.randomUUID().toString().replace("-", "");
        Territory territory = new Territory(territoryId, locationName);
        territory.setWorkspaceId(workspaceId);

        List<String> blocks = requestDto.getBlocks();
        if (blocks != null) {
            for (String block : blocks) {
                if (block != null && !block.trim().isEmpty()) {
                    territory.getBlocks().add(new TerritoryBlock(block.trim(), territory));
                }
            }
        }

        return territoryRepository.save(territory);
    }

    @Transactional
    public List<String> addBlockToTerritory(String territoryId, String blockName) {
        String workspaceId = SecurityAuth.requireWorkspaceId();
        Territory territory = territoryRepository.findById(territoryId.trim())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Territory not found: " + territoryId));

        if (!workspaceId.equals(territory.getWorkspaceId())) {
            throw new jakarta.persistence.EntityNotFoundException(
                    "Territory not found: " + territoryId);
        }

        String trimmed = blockName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Block name is required.");
        }

        boolean alreadyExists = territory.getBlocks().stream()
                .anyMatch(block -> block.getBlockName().equalsIgnoreCase(trimmed));
        if (!alreadyExists) {
            territory.getBlocks().add(new TerritoryBlock(trimmed, territory));
            territoryRepository.save(territory);
        }

        return territory.getBlocks().stream()
                .map(TerritoryBlock::getBlockName)
                .toList();
    }

    @Transactional
    public void softDeleteTerritory(String territoryId) {
        String workspaceId = SecurityAuth.requireWorkspaceId();
        Territory territory = territoryRepository.findById(territoryId.trim())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Territory not found: " + territoryId));
        if (!workspaceId.equals(territory.getWorkspaceId())) {
            throw new jakarta.persistence.EntityNotFoundException("Territory not found: " + territoryId);
        }
        territory.setDeleted(true);
        territoryRepository.save(territory);
    }
}
