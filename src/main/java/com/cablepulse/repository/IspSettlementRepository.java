package com.cablepulse.repository;

import com.cablepulse.model.IspSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IspSettlementRepository extends JpaRepository<IspSettlement, Long> {

    List<IspSettlement> findByWorkspaceIdAndTransactionDateBetween(
            String workspaceId, LocalDateTime start, LocalDateTime end);
}
