-- Assign BASIC (monthly) to operator companies that have no SaaS tier yet.

UPDATE operator_companies oc
SET active_subscription_tier_id = basic_tier.id
FROM (
    SELECT id
    FROM application_subscription_tiers
    WHERE tier_name = 'BASIC'
      AND billing_cycle = 'MONTHLY'
    ORDER BY id
    LIMIT 1
) AS basic_tier
WHERE oc.active_subscription_tier_id IS NULL;
