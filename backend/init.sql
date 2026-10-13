-- Habilitar a extensão geoespacial (PostGIS)
CREATE EXTENSION IF NOT EXISTS postgis;

-- Criação da tabela de alertas
-- O domínio de 'status' no servidor é: 'ativo' | 'removido'.
-- ('pendente' e 'enviado' pertencem ao banco LOCAL do Android (Room) e
--  descrevem o estado de SINCRONIZAÇÃO, não o estado do alerta.)
CREATE TABLE IF NOT EXISTS alertas (
    id           SERIAL PRIMARY KEY,
    tipo_alerta  VARCHAR(50) NOT NULL,          -- 'Alagamento', 'Árvore Caída', 'Trânsito / Bloqueio', ...
    latitude     DOUBLE PRECISION NOT NULL,
    longitude    DOUBLE PRECISION NOT NULL,
    localizacao  GEOMETRY(Point, 4326),         -- SRID 4326 (WGS84 - GPS)
    observacao   VARCHAR(255),
    data_hora    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status       VARCHAR(20) NOT NULL DEFAULT 'ativo',
    usuario      VARCHAR(100) DEFAULT 'Anônimo',
    CONSTRAINT chk_alertas_status CHECK (status IN ('ativo', 'removido'))
);

-- Índice espacial (GiST) que viabiliza o ST_DWithin da rota GET /alertas
CREATE INDEX IF NOT EXISTS idx_alertas_localizacao
    ON alertas USING GIST (localizacao);

-- Índice auxiliar para a listagem do relatório histórico
CREATE INDEX IF NOT EXISTS idx_alertas_data_hora
    ON alertas (data_hora DESC);

-- Preenche a coluna geométrica caso o registro seja inserido direto no banco
-- (fora do worker.py, que já envia o POINT pronto)
CREATE OR REPLACE FUNCTION preencher_localizacao()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.localizacao IS NULL THEN
        NEW.localizacao := ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_preencher_localizacao ON alertas;
CREATE TRIGGER trg_preencher_localizacao
    BEFORE INSERT OR UPDATE ON alertas
    FOR EACH ROW EXECUTE FUNCTION preencher_localizacao();
