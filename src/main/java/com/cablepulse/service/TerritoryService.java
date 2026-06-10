package com.cablepulse.service;

import com.cablepulse.dto.TerritoryRequestDto;
import com.cablepulse.exception.TerritoryAlreadyExistsException;
import com.cablepulse.model.Territory;
import com.cablepulse.model.TerritoryBlock;
import com.cablepulse.repository.TerritoryRepository;
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
        String locationName = requestDto.getLocationName().trim();

        territoryRepository.findByLocationNameIgnoreCase(locationName)
                .ifPresent(existing -> {
                    throw new TerritoryAlreadyExistsException(existing);
                });

        String territoryId = "ter_" + UUID.randomUUID().toString().replace("-", "");
        Territory territory = new Territory(territoryId, locationName);

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

    /**
     * Marks a territory inactive without removing blocks or customer rows.
     * Uses an explicit flag update instead of {@code repository.delete()} so
     * Hibernate does not cascade hard-deletes to child blocks.
     */
    @Transactional
    public void softDeleteTerritory(String territoryId) {
        Territory territory = territoryRepository.findById(territoryId.trim())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Territory not found: " + territoryId));
        territory.setDeleted(true);
        territoryRepository.save(territory);
    }
}
