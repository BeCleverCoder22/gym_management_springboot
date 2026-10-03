CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_organization_slug UNIQUE (slug)
);
CREATE INDEX idx_organization_active ON organizations(active);

INSERT INTO organizations (id, name, slug, active)
VALUES (1, 'Organisation historique', 'legacy-gym', TRUE);
SELECT setval(pg_get_serial_sequence('organizations', 'id'),
              GREATEST((SELECT MAX(id) FROM organizations), 1), TRUE);

ALTER TABLE users ADD COLUMN organization_id BIGINT;
ALTER TABLE customer ADD COLUMN organization_id BIGINT;
ALTER TABLE pack ADD COLUMN organization_id BIGINT;
ALTER TABLE subscription ADD COLUMN organization_id BIGINT;
ALTER TABLE subscription_archive ADD COLUMN organization_id BIGINT;
ALTER TABLE audit_event ADD COLUMN organization_id BIGINT;
ALTER TABLE customer ADD COLUMN IF NOT EXISTS email VARCHAR(255);

UPDATE users SET organization_id = 1;
UPDATE customer SET organization_id = 1;
UPDATE pack SET organization_id = 1;
UPDATE subscription SET organization_id = 1;
UPDATE subscription_archive SET organization_id = 1;
UPDATE audit_event SET organization_id = 1;

ALTER TABLE users ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE customer ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE pack ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE subscription ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE subscription_archive ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE audit_event ALTER COLUMN organization_id SET NOT NULL;

ALTER TABLE users ADD CONSTRAINT fk_users_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);
ALTER TABLE customer ADD CONSTRAINT fk_customer_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);
ALTER TABLE pack ADD CONSTRAINT fk_pack_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);
ALTER TABLE subscription ADD CONSTRAINT fk_subscription_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);
ALTER TABLE subscription_archive ADD CONSTRAINT fk_subscription_archive_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);
ALTER TABLE audit_event ADD CONSTRAINT fk_audit_event_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_username_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
DROP INDEX IF EXISTS uk_users_email_lower;
CREATE UNIQUE INDEX uk_users_org_username ON users(organization_id, username);
CREATE UNIQUE INDEX uk_users_org_email_lower ON users(organization_id, LOWER(email));

CREATE INDEX idx_customer_org_registration ON customer(organization_id, registration_date);
CREATE INDEX idx_customer_org_name ON customer(organization_id, last_name, first_name);
CREATE INDEX idx_customer_org_email ON customer(organization_id, LOWER(email));
CREATE INDEX idx_pack_org_name ON pack(organization_id, LOWER(offer_name));
CREATE INDEX idx_subscription_org_customer_start ON subscription(organization_id, customer_id, start_date);
CREATE INDEX idx_subscription_org_status_end ON subscription(organization_id, status, end_date);
CREATE INDEX idx_subscription_archive_org_start ON subscription_archive(organization_id, start_date);
CREATE INDEX idx_audit_org_occurred ON audit_event(organization_id, occurred_at);

CREATE TABLE payment (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    subscription_id BIGINT NOT NULL REFERENCES subscription(id),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    method VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    provider_reference VARCHAR(160),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    settled_at TIMESTAMP WITH TIME ZONE,
    refunded_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (refunded_amount >= 0),
    CONSTRAINT uk_payment_idempotency UNIQUE (organization_id, idempotency_key)
);
CREATE INDEX idx_payment_org_created ON payment(organization_id, created_at);
CREATE INDEX idx_payment_subscription ON payment(subscription_id);
CREATE UNIQUE INDEX uk_payment_provider_reference
    ON payment(organization_id, provider_reference) WHERE provider_reference IS NOT NULL;

CREATE TABLE payment_refund (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    payment_id BIGINT NOT NULL REFERENCES payment(id),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_refund_payment ON payment_refund(payment_id, status);
CREATE INDEX idx_refund_org_payment ON payment_refund(organization_id, payment_id);

CREATE TABLE notification_outbox (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    event_type VARCHAR(40) NOT NULL,
    deduplication_key VARCHAR(160) NOT NULL,
    recipient VARCHAR(254) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    body VARCHAR(10000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(1000),
    CONSTRAINT uk_notification_dedupe UNIQUE (organization_id, deduplication_key)
);
CREATE INDEX idx_notification_delivery ON notification_outbox(status, next_attempt_at);
CREATE INDEX idx_notification_org_created ON notification_outbox(organization_id, created_at);
