from sqlalchemy import Column, Integer, String, Float, DateTime
from geoalchemy2 import Geometry
from database import Base
import datetime

class Alerta(Base):
    __tablename__ = "alertas"

    id = Column(Integer, primary_key=True, index=True)
    tipo_alerta = Column(String(50), nullable=False)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    observacao = Column(String(255), nullable=True)
    localizacao = Column(Geometry('POINT', srid=4326))
    data_hora = Column(DateTime, default=datetime.datetime.utcnow)
    status = Column(String(20), default="pendente")

