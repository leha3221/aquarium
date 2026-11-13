CREATE TABLE aquariums (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  external_code VARCHAR(32) NOT NULL UNIQUE,
  client_id UUID NOT NULL,
  name VARCHAR(120) NOT NULL CHECK (length(trim(name)) > 0),
  type VARCHAR(24) NOT NULL CHECK (type IN ('FRESHWATER','MARINE','REPTILE','PLANT')),
  volume_liters NUMERIC(10,2) NOT NULL CHECK (volume_liters > 0),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  archived BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_aquarium_client ON aquariums(client_id);
CREATE INDEX ix_aquarium_archived_updated ON aquariums(archived, updated_at);
