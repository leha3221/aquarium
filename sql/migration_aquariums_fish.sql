BEGIN;
CREATE TABLE IF NOT EXISTS aquariums (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL REFERENCES clients(id) ON UPDATE CASCADE ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL CHECK (length(trim(name)) > 0),
    aquarium_type VARCHAR(30) NOT NULL CHECK (aquarium_type IN ('FRESHWATER','MARINE','REPTILE','PLANT')),
    volume_liters NUMERIC(10,2) NOT NULL CHECK (volume_liters > 0)
);
CREATE INDEX IF NOT EXISTS idx_aquariums_client ON aquariums(client_id);
CREATE TABLE IF NOT EXISTS fish (
    id BIGSERIAL PRIMARY KEY,
    aquarium_id BIGINT NOT NULL REFERENCES aquariums(id) ON UPDATE CASCADE ON DELETE CASCADE,
    species VARCHAR(120) NOT NULL CHECK (length(trim(species)) > 0),
    quantity INT NOT NULL CHECK (quantity > 0)
);
CREATE INDEX IF NOT EXISTS idx_fish_aquarium ON fish(aquarium_id);
COMMIT;
