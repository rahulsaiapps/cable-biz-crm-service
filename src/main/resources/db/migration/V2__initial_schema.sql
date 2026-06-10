-- Core Cable Pulse CRM schema (idempotent for fresh and legacy databases).

CREATE TABLE IF NOT EXISTS application_subscription_tiers (
    id                  BIGSERIAL PRIMARY KEY,
    tier_name           VARCHAR(255) NOT NULL,
    billing_cycle       VARCHAR(255) NOT NULL,
    retail_price        DOUBLE PRECISION NOT NULL,
    discounted_price    DOUBLE PRECISION NOT NULL,
    currency_code       VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS connection_providers (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    registered_at   TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS territories (
    territory_id    VARCHAR(255) PRIMARY KEY,
    location_name   VARCHAR(255) NOT NULL,
    district        VARCHAR(255),
    state           VARCHAR(255),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS territory_blocks (
    block_id        BIGSERIAL PRIMARY KEY,
    block_name      VARCHAR(255) NOT NULL,
    territory_id    VARCHAR(255) NOT NULL REFERENCES territories (territory_id)
);

CREATE TABLE IF NOT EXISTS global_plans (
    plan_id         VARCHAR(255) PRIMARY KEY,
    plan_name       VARCHAR(255) NOT NULL,
    monthly_rate    NUMERIC(10, 2) NOT NULL,
    channels_text   TEXT,
    is_hd           BOOLEAN DEFAULT FALSE,
    provider_id     BIGINT REFERENCES connection_providers (id)
);

CREATE TABLE IF NOT EXISTS employees (
    employee_id     VARCHAR(255) PRIMARY KEY,
    full_name       VARCHAR(255) NOT NULL,
    role            VARCHAR(255) NOT NULL,
    email           VARCHAR(255),
    description     TEXT
);

CREATE TABLE IF NOT EXISTS employee_assigned_villages (
    employee_id     VARCHAR(255) NOT NULL REFERENCES employees (employee_id),
    village_name    VARCHAR(255) NOT NULL,
    PRIMARY KEY (employee_id, village_name)
);

CREATE TABLE IF NOT EXISTS operator_companies (
    id                          BIGSERIAL PRIMARY KEY,
    company_name                VARCHAR(255) NOT NULL,
    promotional_trial_active    BOOLEAN DEFAULT FALSE,
    trial_end_date              TIMESTAMP,
    current_billing_amount      DOUBLE PRECISION,
    active_subscription_tier_id BIGINT REFERENCES application_subscription_tiers (id)
);

CREATE TABLE IF NOT EXISTS customers (
    customer_id           VARCHAR(255) PRIMARY KEY,
    serial_number         INTEGER NOT NULL,
    full_name             VARCHAR(255) NOT NULL,
    mobile_number         VARCHAR(255),
    block_name            VARCHAR(255),
    door_number           VARCHAR(255),
    custom_rate_override  NUMERIC(10, 2),
    territory_id          VARCHAR(255) REFERENCES territories (territory_id),
    plan_id               VARCHAR(255) REFERENCES global_plans (plan_id),
    connection_type       VARCHAR(255),
    box_number            VARCHAR(255),
    card_number           VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS customer_ledgers (
    ledger_id       BIGSERIAL PRIMARY KEY,
    billing_month   VARCHAR(255) NOT NULL,
    billing_year    INTEGER NOT NULL,
    status          VARCHAR(255) NOT NULL,
    paid_amount     NUMERIC(10, 2) NOT NULL,
    due_amount      NUMERIC(10, 2) NOT NULL,
    customer_id     VARCHAR(255) NOT NULL REFERENCES customers (customer_id),
    updated_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS daily_expenses (
    id                      BIGSERIAL PRIMARY KEY,
    amount                  NUMERIC(19, 2) NOT NULL,
    description             VARCHAR(255) NOT NULL,
    expense_category        VARCHAR(255) NOT NULL,
    logged_at               TIMESTAMP NOT NULL,
    logged_by_employee_id   VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS daily_transactions (
    transaction_id      VARCHAR(255) PRIMARY KEY,
    amount_collected    NUMERIC(10, 2) NOT NULL,
    mode_of_payment     VARCHAR(255) NOT NULL,
    recorded_at         TIMESTAMP NOT NULL,
    customer_id         VARCHAR(255) NOT NULL REFERENCES customers (customer_id),
    field_agent_id      VARCHAR(255) NOT NULL REFERENCES employees (employee_id)
);

CREATE TABLE IF NOT EXISTS isp_vendor_settlements (
    id                      BIGSERIAL PRIMARY KEY,
    connection_type_name    VARCHAR(255) NOT NULL,
    amount_paid             NUMERIC(19, 2) NOT NULL,
    payment_status          VARCHAR(255) NOT NULL,
    settlement_notes        VARCHAR(255),
    transaction_date        TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS offline_sync_queue (
    event_id            VARCHAR(255) PRIMARY KEY,
    idempotency_token   VARCHAR(100) NOT NULL UNIQUE,
    payload_body        JSONB NOT NULL,
    status              VARCHAR(255) NOT NULL,
    processed_at        TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subscription_upgrade_intents (
    id                          BIGSERIAL PRIMARY KEY,
    tier_name                   VARCHAR(255) NOT NULL,
    billing_cycle               VARCHAR(255) NOT NULL,
    amount                      DOUBLE PRECISION NOT NULL,
    requested_by_employee_id    VARCHAR(255) NOT NULL,
    recorded_at                 TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_customers_territory_id ON customers (territory_id);
CREATE INDEX IF NOT EXISTS idx_customer_ledgers_customer_id ON customer_ledgers (customer_id);
CREATE INDEX IF NOT EXISTS idx_daily_transactions_recorded_at ON daily_transactions (recorded_at);
