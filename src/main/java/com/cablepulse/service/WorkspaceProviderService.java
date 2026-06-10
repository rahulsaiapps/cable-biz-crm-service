package com.cablepulse.service;

import com.cablepulse.dto.ProviderRequestDto;
import com.cablepulse.exception.ProviderCategoryAlreadyExistsException;
import com.cablepulse.model.ConnectionProvider;
import com.cablepulse.repository.ConnectionProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceProviderService {

    private final ConnectionProviderRepository connectionProviderRepository;

    public WorkspaceProviderService(ConnectionProviderRepository connectionProviderRepository) {
        this.connectionProviderRepository = connectionProviderRepository;
    }

    @Transactional
    public ConnectionProvider createProviderCategory(ProviderRequestDto requestDto) {
        String name = requestDto.getName().trim();

        var existing = connectionProviderRepository.findByName(name);
        if (existing.isPresent()) {
            throw new ProviderCategoryAlreadyExistsException(existing.get());
        }
        return connectionProviderRepository.save(new ConnectionProvider(name));
    }
}
