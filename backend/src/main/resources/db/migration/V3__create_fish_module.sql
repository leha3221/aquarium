CREATE TABLE fish_species (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(32) NOT NULL UNIQUE CHECK (code ~ '^[A-Z0-9][A-Z0-9_-]{0,31}$'),
    name VARCHAR(120) NOT NULL CHECK (length(trim(name)) > 0),
    description VARCHAR(1000) NOT NULL DEFAULT '',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_fish_species_name ON fish_species(lower(name));

CREATE TABLE fish (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_code VARCHAR(32) NOT NULL UNIQUE CHECK (external_code ~ '^[A-Z0-9][A-Z0-9_-]{0,31}$'),
    aquarium_id UUID NOT NULL REFERENCES aquariums(id) ON DELETE RESTRICT,
    species_id UUID NOT NULL REFERENCES fish_species(id) ON DELETE RESTRICT,
    name VARCHAR(120) NOT NULL CHECK (length(trim(name)) > 0),
    description VARCHAR(1000) NOT NULL DEFAULT '',
    quantity INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 1000000),
    status VARCHAR(24) NOT NULL CHECK (status IN ('NORMAL','ADAPTING','QUARANTINE','TREATMENT')),
    requires_observation BOOLEAN NOT NULL DEFAULT FALSE,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_fish_aquarium ON fish(aquarium_id);
CREATE INDEX ix_fish_species ON fish(species_id);
CREATE INDEX ix_fish_status ON fish(status);
CREATE INDEX ix_fish_observation ON fish(requires_observation);
CREATE INDEX ix_fish_quantity ON fish(quantity);
CREATE INDEX ix_fish_archive_updated ON fish(archived, updated_at, id);
-- Expression indexes match case-insensitive prefix/equality search. Substring search
-- can be upgraded to pg_trgm when the shared database extension policy is agreed.
CREATE INDEX ix_fish_name ON fish(lower(name));
CREATE INDEX ix_fish_code ON fish(lower(external_code));
CREATE INDEX ix_fish_priority ON fish (
    (CASE WHEN status = 'TREATMENT' THEN 'HIGH'
          WHEN status IN ('ADAPTING','QUARANTINE') OR requires_observation THEN 'MEDIUM'
          ELSE 'LOW' END)
);

CREATE TABLE fish_tags (
    fish_id UUID NOT NULL REFERENCES fish(id) ON DELETE CASCADE,
    tag VARCHAR(40) NOT NULL CHECK (length(trim(tag)) > 0 AND tag = lower(trim(tag))),
    PRIMARY KEY (fish_id, tag)
);
CREATE INDEX ix_fish_tags_tag ON fish_tags(tag, fish_id);

CREATE TABLE fish_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fish_id UUID NOT NULL REFERENCES fish(id) ON DELETE CASCADE,
    text VARCHAR(2000) NOT NULL CHECK (length(trim(text)) > 0),
    author VARCHAR(255) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_fish_notes_history ON fish_notes(fish_id, pinned DESC, created_at DESC, id);

CREATE TABLE fish_import_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner VARCHAR(255) NOT NULL,
    plan TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX ix_fish_import_expiry ON fish_import_batches(expires_at);

-- Protect the old REPTILE business rule in both directions, including changes
-- through the aquarium module. Locking the parent serializes concurrent writes.
CREATE FUNCTION check_fish_aquarium() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent aquariums%ROWTYPE;
BEGIN
    SELECT * INTO parent FROM aquariums WHERE id = NEW.aquarium_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Aquarium not found' USING ERRCODE = '23503';
    END IF;
    IF parent.type = 'REPTILE' OR (NOT NEW.archived AND (NOT parent.active OR parent.archived)) THEN
        RAISE EXCEPTION 'Aquarium cannot contain this fish group' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_fish_aquarium BEFORE INSERT OR UPDATE ON fish
    FOR EACH ROW EXECUTE FUNCTION check_fish_aquarium();

CREATE FUNCTION protect_aquarium_fish() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.type = 'REPTILE' AND EXISTS (SELECT 1 FROM fish WHERE aquarium_id = NEW.id) THEN
        RAISE EXCEPTION 'Aquarium contains fish' USING ERRCODE = '23514';
    END IF;
    IF (NOT NEW.active OR NEW.archived) AND EXISTS (
        SELECT 1 FROM fish WHERE aquarium_id = NEW.id AND NOT archived
    ) THEN
        RAISE EXCEPTION 'Archive fish before disabling or archiving their aquarium' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_protect_aquarium_fish BEFORE UPDATE OF type, active, archived ON aquariums
    FOR EACH ROW EXECUTE FUNCTION protect_aquarium_fish();

INSERT INTO fish_species(code, name, description, created_at, updated_at) VALUES
    ('GUPPY', 'Гуппи', 'Справочная запись вида', now(), now()),
    ('NEON', 'Неон', 'Справочная запись вида', now(), now()),
    ('CLOWNFISH', 'Рыба-клоун', 'Справочная запись вида', now(), now());
