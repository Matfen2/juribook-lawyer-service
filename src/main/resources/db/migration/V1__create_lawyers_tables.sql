-- ═══════════════════════════════════════════════════════════
--  V1__create_lawyers_tables.sql
--  Schéma initial du lawyer-service
--  Ne jamais modifier ce fichier après le premier déploiement
-- ═══════════════════════════════════════════════════════════

-- ── Table des spécialités juridiques (référentiel) ────────
CREATE TABLE specialties (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL UNIQUE,
    slug        VARCHAR(100)    NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- ── Table des profils avocats ──────────────────────────────
CREATE TABLE lawyers (
    id               BIGSERIAL       PRIMARY KEY,
    auth_user_id     BIGINT          NOT NULL UNIQUE,
    bar_number       VARCHAR(5)      NOT NULL UNIQUE,

    -- Informations professionnelles
    bio              TEXT,
    hourly_rate      INTEGER,
    years_experience INTEGER,
    languages        VARCHAR(200),

    -- Adresse du cabinet (@Embedded dans Lawyer.java)
    street           VARCHAR(200),
    city             VARCHAR(100)    NOT NULL,
    postal_code      VARCHAR(5),
    department       VARCHAR(100),
    region           VARCHAR(100),

    -- Disponibilité et réputation
    available        BOOLEAN         NOT NULL DEFAULT TRUE,
    -- FLOAT8 = double precision → correspond à Double Java (Hibernate attend float(53))
    average_rating   FLOAT8,
    review_count     INTEGER         NOT NULL DEFAULT 0,

    -- Audit
    created_at       TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- ── Table de jointure avocat ↔ spécialité ─────────────────
CREATE TABLE lawyer_specialties (
    lawyer_id    BIGINT  NOT NULL REFERENCES lawyers(id)     ON DELETE CASCADE,
    specialty_id BIGINT  NOT NULL REFERENCES specialties(id) ON DELETE RESTRICT,
    PRIMARY KEY (lawyer_id, specialty_id)
);

-- ── Index pour les recherches fréquentes ──────────────────
CREATE INDEX idx_lawyers_city        ON lawyers (city);
CREATE INDEX idx_lawyers_available   ON lawyers (available);
CREATE INDEX idx_lawyers_auth_user   ON lawyers (auth_user_id);
CREATE INDEX idx_lawyers_bar_number  ON lawyers (bar_number);
CREATE INDEX idx_ls_specialty        ON lawyer_specialties (specialty_id);