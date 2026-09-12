-- Habilitar a extensão geoespacial (PostGIS)
CREATE EXTENSION IF NOT EXISTS postgis;

-- Criação da tabela de alertas
CREATE TABLE IF NOT EXISTS alertas (
    id SERIAL PRIMARY KEY,
    tipo_alerta VARCHAR(50) NOT NULL, -- 'alagamento', 'arvore_caida', 'transito'
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    localizacao GEOMETRY(Point, 4326), -- Opcional, coluna espacial. 4326 é o SRID (WGS84 - GPS)
    data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'pendente' -- 'pendente', 'enviado', 'resolvido'
);

-- Trigger ou Função opcional para preencher a coluna GEOMETRY a partir da latitude e longitude, se necessário
-- (Ou faremos isso no código Python (GeoAlchemy) no insert)

