package com.cablepulse.repository;

import com.cablepulse.model.ApplicationSubscriptionTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationSubscriptionTierRepository extends JpaRepository<ApplicationSubscriptionTier, Long> {
}
