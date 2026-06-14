-- Canonical Cable Pulse app subscription tiers (SaaS billing).
-- Replaces any legacy STANDARD rows with BASIC pricing.

DELETE FROM application_subscription_tiers
WHERE tier_name IN ('STANDARD', 'BASIC', 'PRO');

INSERT INTO application_subscription_tiers (tier_name, billing_cycle, retail_price, discounted_price, currency_code)
VALUES
    ('BASIC', 'MONTHLY', 149, 99, 'INR'),
    ('BASIC', 'YEARLY', 1490, 990, 'INR'),
    ('PRO', 'MONTHLY', 249, 199, 'INR'),
    ('PRO', 'YEARLY', 2490, 1990, 'INR');
