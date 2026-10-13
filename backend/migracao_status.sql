-- Migração para bancos JÁ criados com o init.sql antigo.
-- O docker-compose só executa o init.sql em volume novo, então o banco em uso
-- não recebe as correções sozinho. Rode este script uma única vez.
--
--   docker exec -i flooding_radar_db psql -U postgres -d flooding_radar < migracao_status.sql

BEGIN;

-- 1. Colunas que existiam em models.py mas não no init.sql
ALTER TABLE alertas ADD COLUMN IF NOT EXISTS observacao VARCHAR(255);
ALTER TABLE alertas ADD COLUMN IF NOT EXISTS usuario VARCHAR(100) DEFAULT 'Anônimo';

-- 2. Normaliza registros legados ('pendente', 'enviado', NULL) para 'ativo'
UPDATE alertas
   SET status = 'ativo'
 WHERE status IS NULL
    OR status NOT IN ('ativo', 'removido');

-- 3. Corrige o default e passa a exigir o valor
ALTER TABLE alertas ALTER COLUMN status SET DEFAULT 'ativo';
ALTER TABLE alertas ALTER COLUMN status SET NOT NULL;

ALTER TABLE alertas DROP CONSTRAINT IF EXISTS chk_alertas_status;
ALTER TABLE alertas ADD CONSTRAINT chk_alertas_status
    CHECK (status IN ('ativo', 'removido'));

-- 4. Preenche a geometria de registros inseridos sem ela
UPDATE alertas
   SET localizacao = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
 WHERE localizacao IS NULL
   AND latitude IS NOT NULL
   AND longitude IS NOT NULL;

-- 5. Índices
CREATE INDEX IF NOT EXISTS idx_alertas_localizacao ON alertas USING GIST (localizacao);
CREATE INDEX IF NOT EXISTS idx_alertas_data_hora   ON alertas (data_hora DESC);

COMMIT;

-- Conferência
SELECT status, COUNT(*) FROM alertas GROUP BY status ORDER BY status;
