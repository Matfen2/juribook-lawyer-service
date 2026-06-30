-- ═══════════════════════════════════════════════════════════
--  Ajout de name sur lawyers — dénormalisation depuis
--  User.name de l'auth-service (Sprint 3.6)
--
--  Permet d'afficher le nom de l'avocat sur les cartes de
--  recherche et la fiche détail sans appel inter-services.
-- ═══════════════════════════════════════════════════════════

-- Ajout en NULL d'abord, pour ne pas casser les lignes existantes
ALTER TABLE lawyers
    ADD COLUMN name VARCHAR(100);

-- Backfill temporaire pour les profils déjà créés avant cette migration —
-- ⚠️ À METTRE À JOUR MANUELLEMENT avec le vrai nom de chaque avocat existant :
--   UPDATE lawyers SET name = 'Maître Sophie Martin' WHERE bar_number = '75001';
--   UPDATE lawyers SET name = 'Maître Thomas Leblanc' WHERE bar_number = '69001';
UPDATE lawyers SET name = 'Nom à renseigner' WHERE name IS NULL;

-- Contrainte NOT NULL appliquée après le backfill
ALTER TABLE lawyers
    ALTER COLUMN name SET NOT NULL;
