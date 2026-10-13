from sqlalchemy import Column, Integer, String, Float, DateTime
from geoalchemy2 import Geometry
from database import Base
import datetime

# Domínio de status no servidor: 'ativo' (visível) | 'removido' (soft delete).
# Os estados 'pendente'/'enviado' existem apenas no Room (banco local do app)
# e descrevem o estado de sincronização, não o estado do alerta.
STATUS_ATIVO = "ativo"
STATUS_REMOVIDO = "removido"


class Alerta(Base):
    __tablename__ = "alertas"

    id = Column(Integer, primary_key=True, index=True)
    tipo_alerta = Column(String(50), nullable=False)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    observacao = Column(String(255), nullable=True)
    localizacao = Column(Geometry('POINT', srid=4326))
    data_hora = Column(DateTime, default=datetime.datetime.utcnow)
    status = Column(String(20), nullable=False, default=STATUS_ATIVO)
    usuario = Column(String(100), default="Anônimo")
