package com.cablepulse.repository;

import com.cablepulse.model.SubscriptionUpgradeIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionUpgradeIntentRepository extends JpaRepository<SubscriptionUpgradeIntent, Long> {
}
