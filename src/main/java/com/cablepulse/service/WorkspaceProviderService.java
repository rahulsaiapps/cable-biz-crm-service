package com.cablepulse.service;

import com.cablepulse.dto.ProviderRequestDto;
import com.cablepulse.exception.ProviderCategoryAlreadyExistsException;
import com.cablepulse.exception.TerritoryAlreadyExistsException;
import com.cablepulse.model.ConnectionProvider;
import com.cablepulse.model.Territory;
import com.cablepulse.model.TerritoryBlock;
import com.cablepulse.repository.ConnectionProviderRepository;
import com.cablepulse.repository.TerritoryRepository;
import com.cablepulse.security.SecurityAuth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceProviderService {

    private final ConnectionProviderRepository connectionProviderRepository;
    private final TerritoryRepository territoryRepository;

    public WorkspaceProviderService(
            ConnectionProviderRepository connectionProviderRepository,
            TerritoryRepository territoryRepository) {
        this.connectionProviderRepository = connectionProviderRepository;
        this.territoryRepository = territoryRepository;
    }

    @Transactional
    public Object createWorkspaceProvider(ProviderRequestDto requestDto) {
        if (requestDto.isTerritoryRequest()) {
            return createTerritoryLocation(requestDto);
        }
        return createProviderCategory(requestDto);
    }

    @Transactional
    public ConnectionProvider createProviderCategory(ProviderRequestDto requestDto) {
        String workspaceId = SecurityAuth.requireWorkspaceId();
        if (requestDto.getName() == null || requestDto.getName().isBlank()) {
            throw new IllegalArgumentException("Provider name is required and cannot be blank");
        }

        String name = requestDto.getName().trim();

        var existing = connectionProviderRepository.findByWorkspaceIdAndNameIgnoreCase(workspaceId, name);
        if (existing.isPresent()) {
            throw new ProviderCategoryAlreadyExistsException(existing.get());
        }
        return connectionProviderRepository.save(new ConnectionProvider(workspaceId, name));
    }

    @Transactional
    public Territory createTerritoryLocation(ProviderRequestDto requestDto) {
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
                if (block != null && !block.isBlank()) {
                    territory.getBlocks().add(new TerritoryBlock(block.trim(), territory));
                }
            }
        }

        return territoryRepository.save(territory);
    }
}
