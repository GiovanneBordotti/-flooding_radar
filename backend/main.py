from fastapi import FastAPI, Depends, HTTPException
from sqlalchemy.orm import Session
from database import SessionLocal, engine
import models, schemas

# Garante que as tabelas existem no banco
models.Base.metadata.create_all(bind=engine)

app = FastAPI(title="Radar de Alagamentos API")

# Dependência do BD
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@app.get("/health")
def health_check():
    return {"status": "ok", "message": "O servidor está rodando e conectado ao BD!"}

@app.post("/alertas", response_model=schemas.AlertaResponse)
def criar_alerta(alerta: schemas.AlertaCreate, db: Session = Depends(get_db)):
    # Criar geometria espacial Point WKT: POINT(longitude latitude)
    ponto_wkt = f"POINT({alerta.longitude} {alerta.latitude})"
    
    novo_alerta = models.Alerta(
        tipo_alerta=alerta.tipo_alerta,
        latitude=alerta.latitude,
        longitude=alerta.longitude,
        observacao=alerta.observacao,
        localizacao=ponto_wkt
    )
    db.add(novo_alerta)
    db.commit()
    db.refresh(novo_alerta)
    return novo_alerta

from typing import List, Optional
from sqlalchemy import func

@app.get("/alertas", response_model=List[schemas.AlertaResponse])
def listar_alertas(
    lat: Optional[float] = None, 
    lng: Optional[float] = None, 
    raio_km: float = 5.0, 
    db: Session = Depends(get_db)
):
    query = db.query(models.Alerta).filter(models.Alerta.status == "pendente")
    
    if lat is not None and lng is not None:
        # PostGIS: Filtra por Raio em KM usando Casting para Geography
        # ST_DWithin(geography, geography, metros)
        ponto_usuario = f"SRID=4326;POINT({lng} {lat})"
        query = query.filter(
            func.ST_DWithin(
                func.cast(models.Alerta.localizacao, func.Geography()), 
                func.cast(func.ST_GeomFromEWKT(ponto_usuario), func.Geography()), 
                raio_km * 1000
            )
        )
        
    return query.all()


