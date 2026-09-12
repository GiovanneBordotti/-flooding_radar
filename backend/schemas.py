from pydantic import BaseModel
from datetime import datetime

class AlertaCreate(BaseModel):
    tipo_alerta: str
    latitude: float
    longitude: float
    observacao: str | None = None

class AlertaResponse(AlertaCreate):
    id: int
    data_hora: datetime
    status: str

    class Config:
        from_attributes = True

