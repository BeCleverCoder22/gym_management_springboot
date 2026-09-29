CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    last_login TIMESTAMP,
    enabled BOOLEAN DEFAULT TRUE
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_failed_login_at TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;
UPDATE users SET enabled = TRUE WHERE enabled IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email_lower ON users (LOWER(email));
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_users_role') THEN
        ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'));
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS customer (
    id BIGSERIAL PRIMARY KEY,
    last_name VARCHAR(255),
    first_name VARCHAR(255),
    registration_date DATE NOT NULL DEFAULT CURRENT_DATE,
    phone_number VARCHAR(255),
    active_subscription BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE customer ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE customer ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE customer ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
UPDATE customer SET enabled = TRUE WHERE enabled IS NULL;

CREATE TABLE IF NOT EXISTS pack (
    id BIGSERIAL PRIMARY KEY,
    offer_name VARCHAR(255),
    description VARCHAR(1000),
    duration_months INTEGER NOT NULL,
    monthly_price NUMERIC(12, 2) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE pack ADD COLUMN IF NOT EXISTS description VARCHAR(1000);
ALTER TABLE pack ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE pack ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE pack ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE pack ALTER COLUMN monthly_price TYPE NUMERIC(12, 2)
    USING monthly_price::NUMERIC(12, 2);
UPDATE pack SET active = TRUE WHERE active IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_pack_terms') THEN
        ALTER TABLE pack ADD CONSTRAINT ck_pack_terms
            CHECK (duration_months > 0 AND monthly_price >= 0);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS subscription (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    pack_id BIGINT NOT NULL REFERENCES pack(id),
    start_date DATE NOT NULL,
    end_date DATE,
    duration_months_at_purchase INTEGER,
    monthly_price_at_purchase NUMERIC(12, 2),
    offer_name_at_purchase VARCHAR(100),
    status VARCHAR(20)
);

ALTER TABLE subscription ADD COLUMN IF NOT EXISTS end_date DATE;
ALTER TABLE subscription ADD COLUMN IF NOT EXISTS duration_months_at_purchase INTEGER;
ALTER TABLE subscription ADD COLUMN IF NOT EXISTS monthly_price_at_purchase NUMERIC(12, 2);
ALTER TABLE subscription ADD COLUMN IF NOT EXISTS offer_name_at_purchase VARCHAR(100);
ALTER TABLE subscription ADD COLUMN IF NOT EXISTS status VARCHAR(20);

UPDATE subscription s
SET duration_months_at_purchase = p.duration_months,
    monthly_price_at_purchase = p.monthly_price,
    offer_name_at_purchase = p.offer_name,
    end_date = s.start_date + make_interval(months => p.duration_months)
FROM pack p
WHERE s.pack_id = p.id
  AND s.start_date IS NOT NULL
  AND (s.duration_months_at_purchase IS NULL
       OR s.monthly_price_at_purchase IS NULL
       OR s.offer_name_at_purchase IS NULL
       OR s.end_date IS NULL);

UPDATE subscription
SET status = CASE
    WHEN end_date < CURRENT_DATE THEN 'EXPIRED'
    WHEN start_date > CURRENT_DATE THEN 'SCHEDULED'
    ELSE 'ACTIVE'
END
WHERE status IS NULL AND end_date IS NOT NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM subscription WHERE start_date IS NULL) THEN
        RAISE EXCEPTION 'Cannot migrate subscriptions with a missing start_date';
    END IF;
END $$;
ALTER TABLE subscription ALTER COLUMN start_date SET NOT NULL;

UPDATE customer c
SET active_subscription = EXISTS (
    SELECT 1 FROM subscription s
    WHERE s.customer_id = c.id
      AND s.status = 'ACTIVE'
      AND s.start_date <= CURRENT_DATE
      AND s.end_date >= CURRENT_DATE
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_subscription_status') THEN
        ALTER TABLE subscription ADD CONSTRAINT ck_subscription_status
            CHECK (status IS NULL OR status IN ('SCHEDULED', 'ACTIVE', 'EXPIRED', 'CANCELLED'));
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS subscription_archive (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customer(id),
    pack_id BIGINT NOT NULL REFERENCES pack(id),
    start_date DATE,
    end_date DATE,
    is_deleted BOOLEAN,
    deletion_date DATE,
    offer_name_at_purchase VARCHAR(255),
    monthly_price_at_purchase NUMERIC(12, 2),
    duration_months_at_purchase INTEGER,
    status VARCHAR(20)
);

ALTER TABLE subscription_archive ADD COLUMN IF NOT EXISTS offer_name_at_purchase VARCHAR(255);
ALTER TABLE subscription_archive ADD COLUMN IF NOT EXISTS monthly_price_at_purchase NUMERIC(12, 2);
ALTER TABLE subscription_archive ADD COLUMN IF NOT EXISTS duration_months_at_purchase INTEGER;
ALTER TABLE subscription_archive ADD COLUMN IF NOT EXISTS status VARCHAR(20);
UPDATE subscription_archive a
SET offer_name_at_purchase = p.offer_name,
    monthly_price_at_purchase = p.monthly_price,
    duration_months_at_purchase = p.duration_months,
    status = COALESCE(a.status, 'CANCELLED')
FROM pack p
WHERE a.pack_id = p.id
  AND (a.offer_name_at_purchase IS NULL
       OR a.monthly_price_at_purchase IS NULL
       OR a.duration_months_at_purchase IS NULL
       OR a.status IS NULL);

CREATE INDEX IF NOT EXISTS idx_customer_registration_date ON customer(registration_date);
CREATE INDEX IF NOT EXISTS idx_customer_name ON customer(last_name, first_name);
CREATE INDEX IF NOT EXISTS idx_pack_active ON pack(active);
CREATE INDEX IF NOT EXISTS idx_subscription_customer_start ON subscription(customer_id, start_date);
CREATE INDEX IF NOT EXISTS idx_subscription_status_end ON subscription(status, end_date);
CREATE INDEX IF NOT EXISTS idx_subscription_start_date ON subscription(start_date);
CREATE INDEX IF NOT EXISTS idx_subscription_archive_start_date ON subscription_archive(start_date);

CREATE TABLE IF NOT EXISTS audit_event (
    id BIGSERIAL PRIMARY KEY,
    actor VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_occurred_at ON audit_event(occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_resource ON audit_event(resource_type, resource_id);
