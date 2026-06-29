-- ═══════════════════════════════════════════════════════════
--  V2__insert_specialties.sql
--  Données de référence - 15 spécialités du barreau français
--  Ces données sont immuables (table de référentiel)
-- ═══════════════════════════════════════════════════════════

INSERT INTO specialties (name, slug, description) VALUES
    ('Droit du travail',         'droit-du-travail',         'Contrats, licenciements, prud''hommes, harcèlement au travail'),
    ('Droit de la famille',      'droit-de-la-famille',      'Divorce, garde d''enfants, pension alimentaire, succession'),
    ('Droit pénal',              'droit-penal',              'Défense en matière criminelle et correctionnelle'),
    ('Droit immobilier',         'droit-immobilier',         'Achat, vente, bail, copropriété, construction'),
    ('Droit des affaires',       'droit-des-affaires',       'Création d''entreprise, contrats commerciaux, fusions-acquisitions'),
    ('Droit fiscal',             'droit-fiscal',             'Optimisation fiscale, litiges avec l''administration fiscale'),
    ('Droit de la consommation', 'droit-de-la-consommation', 'Litiges avec des professionnels, clauses abusives'),
    ('Droit administratif',      'droit-administratif',      'Recours contre l''État, permis de construire, marchés publics'),
    ('Droit des étrangers',      'droit-des-etrangers',      'Titres de séjour, naturalisation, asile'),
    ('Droit de la santé',        'droit-de-la-sante',        'Responsabilité médicale, accidents iatrogènes, ONIAM'),
    ('Droit des successions',    'droit-des-successions',    'Héritage, testament, partage, donation'),
    ('Droit des contrats',       'droit-des-contrats',       'Rédaction et contestation de contrats civils et commerciaux'),
    ('Droit de la propriété intellectuelle', 'droit-propriete-intellectuelle', 'Brevets, marques, droits d''auteur, concurrence déloyale'),
    ('Droit bancaire',           'droit-bancaire',           'Crédits, saisies, surendettement, fraude bancaire'),
    ('Droit de l''environnement','droit-environnement',       'Pollution, urbanisme, énergie, responsabilité environnementale');