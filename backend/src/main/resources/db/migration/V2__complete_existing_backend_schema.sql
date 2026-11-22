-- Missing tables for the existing Client and ServiceRequest JPA entities.
-- V1 is intentionally unchanged: already applied Flyway migrations are immutable.
CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(120) NOT NULL CHECK (length(trim(full_name)) > 0),
    phone VARCHAR(30) NOT NULL UNIQUE,
    email VARCHAR(160) UNIQUE,
    address VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_clients_name ON clients(full_name);

ALTER TABLE aquariums ADD CONSTRAINT fk_aquarium_client
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT;

CREATE TABLE service_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_code VARCHAR(32) NOT NULL UNIQUE,
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE RESTRICT,
    aquarium_id UUID NOT NULL REFERENCES aquariums(id) ON DELETE RESTRICT,
    aquarium_type VARCHAR(24) NOT NULL CHECK (aquarium_type IN ('FRESHWATER','MARINE','REPTILE','PLANT')),
    service_type VARCHAR(32) NOT NULL CHECK (service_type IN ('CLEANING','WATER_CHANGE','EQUIPMENT_REPAIR','FISH_HEALTH','AQUASCAPING')),
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(24) NOT NULL CHECK (status IN ('NEW','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED')),
    priority INTEGER NOT NULL CHECK (priority BETWEEN 1 AND 5),
    scheduled_at TIMESTAMP,
    price NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_requests_client ON service_requests(client_id);
CREATE INDEX ix_requests_status_created ON service_requests(status, created_at);
